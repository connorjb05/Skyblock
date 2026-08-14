package me.connorjb05.skyblock.service.island.deletion;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.repository.IslandDeletionRepository;
import me.connorjb05.skyblock.database.repository.IslandPlayerRepository;
import me.connorjb05.skyblock.database.repository.IslandRepository;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.IslandCache;
import me.connorjb05.skyblock.model.island.IslandDeletion;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.service.island.IslandSlotService;
import me.connorjb05.skyblock.service.island.deletion.wiper.IslandWiper;
import me.connorjb05.skyblock.util.ExceptionUtil;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class IslandDeletionService {

    private final IslandSlotService slotService;

    private final IslandRepository islandRepository;
    private final IslandDeletionRepository deletionRepository;
    private final IslandPlayerRepository playerRepository;

    private final IslandWiper islandWiper;

    private final IslandCache cache;

    /*
    PriorityBlockingQueue because deletion entries
    could be added from asynchronous database callbacks.
     */
    private final PriorityBlockingQueue<IslandDeletion> deletionQueue =
            new PriorityBlockingQueue<>(
                    11,
                    Comparator
                            .comparingInt(IslandDeletion::getPriority)
                            .reversed()
                            .thenComparingLong(IslandDeletion::getQueuedAt)
            );

    /*
    Only one physical island cleanup should
    execute at a time
     */
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private BukkitTask processingTask;

    public IslandDeletionService(
            IslandSlotService slotService,
            IslandRepository islandRepository,
            IslandDeletionRepository deletionRepository,
            IslandPlayerRepository playerRepository,
            IslandWiper islandWiper,
            IslandCache cache
    ) {
        this.slotService = slotService;
        this.islandRepository = islandRepository;
        this.deletionRepository = deletionRepository;
        this.playerRepository = playerRepository;
        this.islandWiper = islandWiper;
        this.cache = cache;
    }

    /*
    Restores persisted deletions when the
    server starts.
     */
    public void populate(Collection<IslandDeletion> deletions) {
        deletionQueue.clear();
        deletionQueue.addAll(deletions);
    }

    /*
    Starts periodically checking the deletion queue.

    Only one island will be processed at once.
     */
    public void start(Skyblock plugin) {

        if (processingTask != null) {
            return;
        }

        processingTask = plugin
                .getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        this::processNext,
                        20L,
                        100L
                );
    }

    public void stop() {

        if (processingTask == null) {
            return;
        }

        processingTask.cancel();
        processingTask = null;
    }

    /*
    Queues an island for deletion.

    This does NOT release its slot!
     */
    public CompletableFuture<IslandDeletionResult> delete(
            UUID executorId,
            Island island
    ) {

        PlayerData playerData = Skyblock.get().getPlayerDataService().get(executorId);

        if (island == null || !playerData.hasIsland()) {
            return CompletableFuture.completedFuture(
                    IslandDeletionResult.failure(IslandDeletionStatus.NOT_IN_ISLAND)
            );
        }

        if (!island.getLeaderId().equals(executorId)) {
            return CompletableFuture.completedFuture(
                    IslandDeletionResult.failure(IslandDeletionStatus.NOT_OWNER)
            );
        }

        return deletionRepository
                .exists(island.getIslandId())
                .thenCompose(exists -> {

                    if (exists) {
                        return CompletableFuture.completedFuture(
                                IslandDeletionResult.failure(IslandDeletionStatus.ALREADY_QUEUED)
                        );
                    }

                    return queueDeletion(island);
                });
    }

    private CompletableFuture<IslandDeletionResult> queueDeletion(
            Island island
    ) {

        IslandDeletion deletion =
                new IslandDeletion(
                        island.getIslandId(),
                        island.getSlotId(),
                        0,
                        System.currentTimeMillis()
                );

        /*
         * Read memberships BEFORE the island
         * is deleted.
         *
         * ON DELETE CASCADE will remove them
         * when the island row disappears.
         */
        return playerRepository
                .findByIsland(island.getIslandId())
                .thenCompose(islandMembers ->

                        /*
                         * Persist deletion marker first.
                         */
                        deletionRepository
                                .create(deletion)

                                /*
                                 * Remove active island.
                                 *
                                 * This also removes
                                 * skyblock_players rows through
                                 * ON DELETE CASCADE.
                                 */
                                .thenCompose(ignored ->
                                        islandRepository
                                                .delete(island.getIslandId())
                                                .thenApply(deleted -> {

                                                    /*
                                                     * Island is no longer active.
                                                     */
                                                    cache.remove(
                                                            island
                                                    );

                                                    /*
                                                     * Clear runtime membership
                                                     * for EVERY island member.
                                                     */
                                                    for (IslandMember islandMember : islandMembers) {

                                                        PlayerData playerData =
                                                                Skyblock.get()
                                                                        .getPlayerDataService()
                                                                        .get(islandMember.playerId());

                                                        if (playerData != null) {
                                                            playerData.setIslandId(-1L);
                                                            playerData.setIslandMember(null);
                                                        }
                                                    }

                                                    /*
                                                     * Physical slot is STILL
                                                     * held hostage.
                                                     */
                                                    deletionQueue.offer(
                                                            deletion
                                                    );

                                                    return IslandDeletionResult.queued(
                                                            deletion
                                                    );
                                                })

                                                .exceptionallyCompose(throwable ->
                                                        rollbackDeletionMarker(
                                                                deletion,
                                                                throwable
                                                        )
                                                )
                                )
                );
    }

    private CompletableFuture<IslandDeletionResult> rollbackDeletionMarker(
            IslandDeletion deletion,
            Throwable originalThrowable
    ) {
        return deletionRepository
                .delete(
                        deletion.getIslandId()
                )
                .handle((ignored, rollbackThrowable) -> {

                    if (rollbackThrowable != null) {
                        Skyblock.get()
                                .getLogger()
                                .severe(
                                        "Failed to rollback deletion marker for island "
                                                + deletion.getIslandId()
                                                + ". Manual database cleanup may be required."
                                );
                    }

                    throw new CompletionException(
                            ExceptionUtil.unwrap(
                                    originalThrowable
                            )
                    );
                });
    }

    /*
    Processes the highest priority deletion.
     */
    private void processNext() {

        if (!processing.compareAndSet(
                false,
                true
        )) {
            return;
        }

        IslandDeletion deletion = deletionQueue.peek();

        if (deletion == null) {
            processing.set(false);
            return;
        }

        /*
        Delete from active islands again.

        DELETE is effectively idempotent here,
        and this makes crash recovery safer.
         */
        islandRepository
                .delete(deletion.getIslandId())
                /*
                Physically remove the island
                 */
                .thenCompose(ignored -> islandWiper.wipe(deletion))
                /*
                Physical deletion successful.
                We can now remove the persistent deletion
                queue entry.
                 */
                .thenCompose(ignored -> deletionRepository.delete(deletion.getIslandId()))
                .whenComplete((ignored, throwable) -> {

                    if (throwable != null) {
                        Throwable cause = ExceptionUtil.unwrap(throwable);

                        Skyblock.get()
                                .getLogger()
                                .severe(
                                        "Failed to completely delete island " +
                                                deletion.getIslandId() +
                                                ": " +
                                                cause.getMessage()
                                );

                        /*
                        Keep it in the queue.

                        It can be retried on the next processing
                        cycle.
                         */
                        processing.set(false);
                        return;
                    }

                    /*
                    NOW the island is truly gone.
                     */
                    deletionQueue.remove(deletion);

                    /*
                    Only release the slot after:

                    1. active island row removed
                    2. physical island wiped
                    3. deletion row removed
                     */
                    slotService.releaseSlot(deletion.getSlotId());

                    processing.set(false);
                });
    }

    /*
    Queues cleanup for an island that failed
    while being physically created.

    The slot remains reserved until the
    physical wipe successfully completes.
     */
    public CompletableFuture<Void> cleanupFailedCreation(
            Island island
    ) {

        return deletionRepository
                .exists(island.getIslandId())
                .thenCompose(exists -> {

                    if (exists) {

                        /*
                         * A persistent deletion marker
                         * already protects this slot.
                         */
                        return CompletableFuture.completedFuture(
                                null
                        );
                    }

                    return queueDeletion(island)
                            .thenApply(ignored -> null);
                });
    }

    public int getQueueSize() {
        return deletionQueue.size();
    }


    public boolean isProcessing() {
        return processing.get();
    }
}
