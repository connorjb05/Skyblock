package me.connorjb05.skyblock.service.island;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.repository.IslandPlayerRepository;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.*;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.service.island.membership.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandMembershipService {

    private final IslandPlayerRepository playerRepository;
    private final IslandCache islandCache;

    public IslandMembershipService(
            IslandPlayerRepository playerRepository,
            IslandCache islandCache
    ) {
        this.playerRepository = playerRepository;
        this.islandCache = islandCache;
    }

    /**
     * Accepts an island invitation.
     *
     * @param playerId player accepting the invite
     * @param islandId island they were invited to
     * @return join result
     */
    public CompletableFuture<IslandJoinResult> acceptInvite(
            UUID playerId,
            long islandId
    ) {

        PlayerData playerData = Skyblock.get()
                .getPlayerDataService()
                .get(playerId);

        if (playerData == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "PlayerData is not loaded for " + playerId
                    )
            );
        }

        if (playerData.hasIsland()) {
            return CompletableFuture.completedFuture(
                    IslandJoinResult.failure(
                            IslandJoinStatus.ALREADY_IN_ISLAND
                    )
            );
        }

        Optional<IslandInvite> inviteOptional = playerData.getInvite(islandId);

        if (inviteOptional.isEmpty()) {
            return CompletableFuture.completedFuture(
                    IslandJoinResult.failure(
                            IslandJoinStatus.INVITE_NOT_FOUND
                    )
            );
        }

        IslandInvite invite = inviteOptional.get();

        /*
        Expired invitations are removed immediately
         */
        if (invite.expired(
                Skyblock.get()
                .getConfigService()
                .getIslandInviteSettings()
                .duration())
        ) {
            playerData.removeInvite(islandId);

            return CompletableFuture.completedFuture(
                    IslandJoinResult.failure(
                            IslandJoinStatus.INVITE_EXPIRED
                    )
            );
        }

        /*
        An invitation may still exist after the island
        was deleted.

        IslandCache only contains ACTIVE islands.
         */
        Optional<Island> islandOptional = islandCache.findById(islandId);

        if (islandOptional.isEmpty()) {
            playerData.removeInvite(islandId);

            return CompletableFuture.completedFuture(
                    IslandJoinResult.failure(
                            IslandJoinStatus.ISLAND_NOT_FOUND
                    )
            );
        }

        Island island = islandOptional.get();

        /*
        Check persistent membership as well.

        PlayerData is the fast runtime source,
        but the database remains persistent truth.
         */
        return playerRepository
                .exists(playerId)
                .thenCompose(exists -> {

                    if (exists) {
                        return CompletableFuture.completedFuture(
                                IslandJoinResult.failure(
                                        IslandJoinStatus.ALREADY_IN_ISLAND
                                )
                        );
                    }

                    return join(
                            playerData,
                            island
                    );
                });
    }

    private CompletableFuture<IslandJoinResult> join(
            PlayerData playerData,
            Island island
    ) {
        IslandMember islandMember = new IslandMember(
                playerData.getUuid(),
                island.getIslandId(),
                IslandRole.MEMBER,
                System.currentTimeMillis()
        );

        return playerRepository
                .create(islandMember)
                .thenApply(ignored -> {

                    /*
                    Persistent membership succeeded

                    Update runtime data only AFTER
                    the database operation succeeds.
                     */

                    playerData.setIslandId(island.getIslandId());
                    playerData.setIslandMember(islandMember);

                    /*
                    Once someone joins an island,
                    every other pending invitation
                    becomes irrelevant
                     */
                    playerData.clearInvites();

                    return IslandJoinResult.success(island);
                });
    }

    /*
    Declines one pending invitation
     */
    public IslandDeclineResult declineInvite(
            UUID playerId,
            long islandId
    ) {
        PlayerData playerData = Skyblock.get()
                .getPlayerDataService()
                .get(playerId);

        if (playerData == null) {
            return IslandDeclineResult.failure(
                    IslandDeclineStatus.INVITE_NOT_FOUND
            );
        }

        if (!playerData.hasInvite(islandId)) {
            return IslandDeclineResult.failure(
                    IslandDeclineStatus.INVITE_NOT_FOUND
            );
        }

        playerData.removeInvite(islandId);

        return IslandDeclineResult.success();
    }

    /**
     * Leaves the player's current island.
     *
     * Island owners are NOT allowed to leave.
     *
     * They must either:
     * - disband/delete their island
     * - transfer ownership first
     */
    public CompletableFuture<IslandLeaveResult> leave(UUID playerId) {

        PlayerData playerData = Skyblock.get()
                .getPlayerDataService()
                .get(playerId);

        if (playerData == null || !playerData.hasIsland()) {
            return CompletableFuture.completedFuture(
                    IslandLeaveResult.failure(
                            IslandLeaveStatus.NOT_IN_ISLAND
                    )
            );
        }

        /*
         * Do NOT rely exclusively on PlayerData's role.
         *
         * Check persistent membership before allowing
         * someone to leave.
         */
        return playerRepository
                .find(playerId)
                .thenCompose(optional -> {

                    if (optional.isEmpty()) {

                        /*
                         * Runtime state was stale.
                         */
                        clearIslandData(playerData);

                        return CompletableFuture.completedFuture(
                                IslandLeaveResult.failure(
                                        IslandLeaveStatus.NOT_IN_ISLAND
                                )
                        );
                    }

                    IslandMember islandMember = optional.get();

                    /*
                     * ABSOLUTE RULE:
                     *
                     * The owner cannot leave their island.
                     */
                    if (islandMember.role() == IslandRole.LEADER) {
                        return CompletableFuture.completedFuture(
                                IslandLeaveResult.failure(
                                        IslandLeaveStatus.LEADER_CANNOT_LEAVE
                                )
                        );
                    }

                    return leave(
                            playerData,
                            islandMember
                    );
                });
    }

    private CompletableFuture<IslandLeaveResult> leave(
            PlayerData playerData,
            IslandMember islandMember
    ) {
        return playerRepository
                .delete(
                        islandMember.playerId()
                )
                .thenApply(ignored -> {

                    /*
                     * Membership no longer exists
                     * persistently.
                     *
                     * Player is now immediately free
                     * to join/create another island.
                     */
                    clearIslandData(playerData);

                    return IslandLeaveResult.success();
                });
    }

    private void clearIslandData(
            PlayerData playerData
    ) {

        playerData.setIslandId(
                -1L
        );

        playerData.setIslandMember(
                null
        );
    }
}
