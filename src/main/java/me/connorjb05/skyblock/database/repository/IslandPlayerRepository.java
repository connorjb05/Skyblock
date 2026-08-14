package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.membership.IslandRole;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandPlayerRepository {

    private final Database database;

    public IslandPlayerRepository(Database database) {
        this.database = database;
    }

    /**
     * Adds a player to an island.
     *
     * @param member player membership
     * @return completion future
     */
    public CompletableFuture<Void> create(IslandMember member) {

        String sql = """
                INSERT INTO skyblock_players (
                    player_uuid,
                    island_id,
                    role,
                    time_joined
                )
                VALUES (?, ?, ?, ?)
                """;

        return database
                .getSqlService()
                .pushAsync(
                        sql,
                        List.of(
                                member.playerId().toString(),
                                member.islandId(),
                                member.role().name(),
                                member.timeJoined()
                        )
                )
                .thenApply(ignored -> null);
    }

    /**
     * Finds a player's current island membership.
     *
     * @param playerId player UUID
     * @return player membership
     */
    public CompletableFuture<Optional<IslandMember>> find(UUID playerId) {

        String sql = """
                SELECT
                    player_uuid,
                    island_id,
                    role,
                    time_joined
                FROM skyblock_players
                WHERE player_uuid = ?
                LIMIT 1
                """;

        return database
                .getSqlService()
                .pullAsync(
                        sql,
                        List.of(playerId.toString()),
                        resultSet -> {

                            if (!resultSet.next()) {
                                return Optional.empty();
                            }

                            return Optional.of(mapToPlayer(resultSet));
                        }
                );
    }

    /**
     * Finds all players belonging to an island.
     *
     * @param islandId island ID
     * @return island members
     */
    public CompletableFuture<List<IslandMember>> findByIsland(long islandId) {

        String sql = """
                SELECT
                    player_uuid,
                    island_id,
                    role,
                    time_joined
                FROM skyblock_players
                WHERE island_id = ?
                """;

        return database
                .getSqlService()
                .pullAsync(
                        sql,
                        List.of(islandId),
                        resultSet -> {

                            List<IslandMember> members = new ArrayList<>();

                            while (resultSet.next()) {
                                members.add(mapToPlayer(resultSet));
                            }

                            return members;
                        }
                );
    }

    /**
     * Checks whether a player currently
     * belongs to an island.
     *
     * @param playerId player UUID
     * @return whether the player belongs to an island
     */
    public CompletableFuture<Boolean> exists(UUID playerId) {

        String sql = """
                SELECT 1
                FROM skyblock_players
                WHERE player_uuid = ?
                LIMIT 1
                """;

        return database
                .getSqlService()
                .pullAsync(
                        sql,
                        List.of(playerId.toString()),
                        ResultSet::next
                );
    }

    /**
     * Checks whether a player belongs
     * to a specific island.
     *
     * @param playerId player UUID
     * @param islandId island ID
     * @return whether the player belongs to the island
     */
    public CompletableFuture<Boolean> exists(
            UUID playerId,
            long islandId
    ) {

        String sql = """
                SELECT 1
                FROM skyblock_players
                WHERE player_uuid = ?
                  AND island_id = ?
                LIMIT 1
                """;

        return database
                .getSqlService()
                .pullAsync(
                        sql,
                        List.of(
                                playerId.toString(),
                                islandId
                        ),
                        ResultSet::next
                );
    }

    public CompletableFuture<Void> updateRole(
            UUID playerId,
            IslandRole role
    ) {

        String sql = """
                UPDATE skyblock_players
                SET role = ?
                WHERE player_uuid = ?
                """;

        return database
                .getSqlService()
                .pushAsync(
                        sql,
                        List.of(
                                role.name(),
                                playerId.toString()
                        )
                )
                .thenApply(ignored -> null);
    }

    /**
     * Removes a player from their island.
     *
     * @param playerId player UUID
     * @return completion future
     */
    public CompletableFuture<Void> delete(UUID playerId) {

        String sql = """
                DELETE FROM skyblock_players
                WHERE player_uuid = ?
                """;

        return database
                .getSqlService()
                .pushAsync(
                        sql,
                        List.of(playerId.toString())
                )
                .thenApply(ignored -> null);
    }

    /**
     * Removes every player belonging
     * to an island.
     *
     * Normally ON DELETE CASCADE handles this,
     * but this method can still be useful.
     *
     * @param islandId island ID
     * @return completion future
     */
    public CompletableFuture<Void> deleteByIsland(
            long islandId
    ) {

        String sql = """
                DELETE FROM skyblock_players
                WHERE island_id = ?
                """;

        return database
                .getSqlService()
                .pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    /**
     * Finds every island member.
     *
     * Used during start up to populate
     * the member set of cached islands.
     *
     * @return List of every island member within the table
     */
    public CompletableFuture<List<IslandMember>> findAll() {

        String sql = """
                SELECT
                    island_id,
                    player_uuid,
                    role,
                    time_joined
                FROM skyblock_players
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(),
                resultSet -> {

                    List<IslandMember> members = new ArrayList<>();

                    while (resultSet.next()) {
                        members.add(mapToPlayer(resultSet));
                    }

                    return members;
                }
        );
    }

    private IslandMember mapToPlayer(ResultSet rs) throws SQLException {
        return new IslandMember(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getLong("island_Id"),
                IslandRole.valueOf(rs.getString("role")),
                rs.getLong("time_joined")
        );
    }
}
