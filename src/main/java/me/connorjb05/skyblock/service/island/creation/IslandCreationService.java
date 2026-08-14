package me.connorjb05.skyblock.service.island.creation;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.repository.IslandPlayerRepository;
import me.connorjb05.skyblock.database.repository.IslandRepository;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.IslandCache;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.service.island.IslandSlotService;
import me.connorjb05.skyblock.service.island.creation.schematic.IslandPasteResult;
import me.connorjb05.skyblock.service.island.creation.schematic.IslandSchematicService;
import me.connorjb05.skyblock.service.island.deletion.IslandDeletionService;
import me.connorjb05.skyblock.service.island.teleport.IslandTeleportService;
import me.connorjb05.skyblock.util.ExceptionUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service in charge of island creation.
 */
public class IslandCreationService {

    private final IslandSlotService slotService;
    private final IslandSchematicService schematicService;
    private final IslandTeleportService teleportService;
    private final IslandDeletionService deletionService;

    private final IslandRepository islandRepository;
    private final IslandPlayerRepository playerRepository;

    private final IslandCache cache;

    public IslandCreationService(
            IslandSlotService slotService,
            IslandSchematicService schematicService,
            IslandTeleportService teleportService,
            IslandDeletionService deletionService,
            IslandRepository islandRepository,
            IslandPlayerRepository playerRepository,
            IslandCache cache
    ) {
        this.slotService = slotService;
        this.schematicService = schematicService;
        this.teleportService = teleportService;
        this.deletionService = deletionService;

        this.islandRepository = islandRepository;
        this.playerRepository = playerRepository;

        this.cache = cache;
    }

    /**
     * Creates and persists a new island
     * for the provided owner.
     *
     * @param ownerId island owner's UUID
     * @return future containing the creation result
     */
    public CompletableFuture<IslandCreationResult> create(
            UUID ownerId
    ) {

        PlayerData playerData = Skyblock.get()
                        .getPlayerDataService()
                        .get(ownerId);

        /*
        Fast runtime check.
         */
        if (playerData != null && playerData.hasIsland()) {

            return CompletableFuture.completedFuture(
                    IslandCreationResult.failure(
                            IslandCreationStatus.ALREADY_IN_ISLAND
                    )
            );
        }

        /*
        Check both persistent sources.
         */
        CompletableFuture<Boolean> ownerCheck = islandRepository
                .existsByOwner(ownerId);

        CompletableFuture<Boolean> membershipCheck = playerRepository
                .exists(ownerId);

        return ownerCheck
                .thenCombine(membershipCheck, (ownsIsland, hasMembership) ->
                                ownsIsland || hasMembership
                )
                .thenCompose(exists -> {

                    if (exists) {

                        return CompletableFuture.completedFuture(
                                IslandCreationResult.failure(
                                        IslandCreationStatus.ALREADY_IN_ISLAND
                                )
                        );
                    }

                    return createAndPersist(ownerId);
                });
    }

    private CompletableFuture<IslandCreationResult> createAndPersist(
            UUID ownerId
    ) {

        long slotId = slotService.claimSlot();

        Island island = new Island(
                        0,
                        slotId,
                        ownerId
                );

        /*
        Step 1:

        Create the island database row.
         */
        return islandRepository
                .create(island)

                /*
                Initial island INSERT failed.

                Nothing physical exists yet,
                so the slot can immediately
                be released.
                 */
                .handle((savedIsland, throwable) -> {

                            if (throwable != null) {

                                slotService.releaseSlot(
                                        slotId
                                );

                                throw new CompletionException(
                                        ExceptionUtil.unwrap(throwable)
                                );
                            }

                            return savedIsland;
                        }
                )

                /*
                Step 2:

                Persist the owner's membership.
                 */
                .thenCompose(savedIsland -> createOwnerMembership(
                                ownerId,
                                savedIsland,
                                slotId
                        )
                )

                /*
                Step 3:

                The database records now exist.

                Physically paste the island.
                 */
                .thenCompose(
                        this::pasteIsland
                )


                /*
                Step 4:

                Everything now exists:

                - island row
                - membership row
                - physical island

                Activate the island at runtime.
                 */
                .thenApply(
                        this::activateIsland
                )

                /*
                Step 5:

                Send the owner to their island.

                Teleport failure does NOT destroy
                an otherwise successfully created
                island.
                 */
                .thenCompose(context ->
                        teleportOwner(
                                ownerId,
                                context
                        )
                );
    }

    private CompletableFuture<PersistedCreation> createOwnerMembership(
            UUID ownerId,
            Island island,
            long slotId
    ) {

        IslandMember owner = new IslandMember(
                        ownerId,
                        island.getIslandId(),
                        IslandRole.LEADER,
                        System.currentTimeMillis()
                );

        return playerRepository
                .create(owner)

                /*
                Do NOT update PlayerData here.

                We have not successfully created
                the physical island yet.
                 */
                .thenApply(ignored ->
                        new PersistedCreation(
                                island,
                                owner
                        )
                )

                /*
                Island INSERT succeeded but
                membership INSERT failed.

                Physical island has NOT been
                pasted yet, so normal rollback
                is safe.
                 */
                .exceptionallyCompose(throwable -> rollbackFailedMembership(
                                island,
                                slotId,
                                throwable
                        )
                );
    }

    private CompletableFuture<ProvisionedCreation> pasteIsland(
            PersistedCreation context
    ) {

        return schematicService
                .paste(context.island())

                .thenApply(pasteResult -> new ProvisionedCreation(
                                context.island(),
                                context.owner(),
                                pasteResult
                        )
                )

                /*
                A schematic operation may have
                partially modified the world.

                Therefore DO NOT directly release
                the slot.

                Queue it through the physical
                island deletion system instead.
                 */
                .exceptionallyCompose(throwable -> cleanupFailedPaste(
                                context,
                                throwable
                        )
                );
    }

    private ProvisionedCreation activateIsland(
            ProvisionedCreation context
    ) {

        Island island = context.island();

        cache.add(island);

        PlayerData playerData = Skyblock.get()
                        .getPlayerDataService()
                        .get(island.getLeaderId());

        if (playerData != null) {
            playerData.setIslandId(island.getIslandId());
            playerData.setIslandMember(context.owner());
        }

        return context;
    }

    private CompletableFuture<IslandCreationResult> teleportOwner(
            UUID ownerId,
            ProvisionedCreation context
    ) {

        return teleportService
                .teleport(
                        ownerId,
                        context.pasteResult().spawnLocation()
                )

                .exceptionally(throwable -> {

                    Throwable cause = ExceptionUtil.unwrap(throwable);

                    Skyblock.get()
                            .getLogger()
                            .warning(
                                    "Failed to teleport player "
                                            + ownerId
                                            + " to newly created island "
                                            + context.island()
                                            .getIslandId()
                                            + ": "
                                            + cause.getMessage()
                            );

                    return false;
                })

                .thenApply(ignored ->
                        IslandCreationResult.success(context.island())
                );
    }

    /*
    Database membership creation failed.

    No physical island exists yet,
    so deleting the database row and
    releasing the slot is safe.
     */
    private CompletableFuture<PersistedCreation> rollbackFailedMembership(
            Island island,
            long slotId,
            Throwable originalThrowable
    ) {

        return islandRepository
                .delete(island.getIslandId())
                .handle(
                        (ignored, rollbackThrowable) -> {

                            if (rollbackThrowable == null) {

                                slotService.releaseSlot(slotId);

                            } else {

                                Skyblock.get()
                                        .getLogger()
                                        .severe(
                                                "Failed to rollback island "
                                                        + island.getIslandId()
                                                        + " after owner membership creation failed."
                                        );
                            }

                            throw new CompletionException(
                                    ExceptionUtil.unwrap(originalThrowable)
                            );
                        }
                );
    }

    /*
    Schematic paste failed.

    The world may already contain some
    pasted blocks.

    Send the island through the existing
    physical deletion queue instead of
    immediately recycling the slot.
     */
    private CompletableFuture<ProvisionedCreation> cleanupFailedPaste(
            PersistedCreation context,
            Throwable originalThrowable
    ) {

        Throwable cause =
                ExceptionUtil.unwrap(originalThrowable);

        return deletionService
                .cleanupFailedCreation(context.island())
                .handle(
                        (ignored, cleanupThrowable) -> {

                            if (cleanupThrowable != null) {

                                Throwable cleanupCause = ExceptionUtil.unwrap(cleanupThrowable);

                                Skyblock.get()
                                        .getLogger()
                                        .severe(
                                                "Failed to queue cleanup for partially created island "
                                                        + context.island()
                                                        .getIslandId()
                                                        + ": "
                                                        + cleanupCause.getMessage()
                                        );
                            }

                            throw new CompletionException(
                                    cause
                            );
                        }
                );
    }

    private record PersistedCreation(
            Island island,
            IslandMember owner
    ) {
    }

    private record ProvisionedCreation(
            Island island,
            IslandMember owner,
            IslandPasteResult pasteResult
    ) {
    }
}