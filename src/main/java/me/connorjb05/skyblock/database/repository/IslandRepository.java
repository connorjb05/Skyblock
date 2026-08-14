package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.Island;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandRepository {

    private final Database database;

    public IslandRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Island> create(Island island) {

        String sql = """
                INSERT INTO islands (
                    slot_id,
                    leader_uuid
                )
                VALUES (?, ?)
                """;

        return database.getSqlService().pushKeyAsync(
                        sql,
                        List.of(
                                island.getSlotId(),
                                island.getLeaderId().toString()
                        )
                )
                .thenApply(islandId -> new Island(
                        islandId,
                        island.getSlotId(),
                        island.getLeaderId()
                ));
    }

    public CompletableFuture<Optional<Island>> findById(long islandId) {

        String sql = """
                SELECT
                    id,
                    slot_id,
                    leader_uuid
                FROM islands
                WHERE id = ?
                LIMIT 1
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(islandId),
                resultSet -> {

                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    return Optional.of(
                            mapToIsland(resultSet)
                    );
                }
        );
    }

    public CompletableFuture<Optional<Island>> findBySlotId(long slotId) {

        String sql = """
                SELECT
                    id,
                    slot_id,
                    leader_uuid
                FROM islands
                WHERE slot_id = ?
                LIMIT 1
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(slotId),
                resultSet -> {

                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    return Optional.of(
                            mapToIsland(resultSet)
                    );
                }
        );
    }

    public CompletableFuture<Optional<Island>> findByOwner(UUID ownerId) {

        String sql = """
                SELECT
                    id,
                    slot_id,
                    leader_uuid
                FROM islands
                WHERE leader_uuid = ?
                LIMIT 1
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(ownerId.toString()),
                resultSet -> {

                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    return Optional.of(
                            mapToIsland(resultSet)
                    );
                }
        );
    }

    public CompletableFuture<List<Island>> findAll() {

        String sql = """
                SELECT
                    *
                FROM islands
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(),
                resultSet -> {

                    List<Island> islands = new ArrayList<>();

                    while (resultSet.next()) {
                        islands.add(mapToIsland(resultSet));
                    }

                    return islands;
                }
        );
    }

    public CompletableFuture<Boolean> existsByOwner(UUID ownerId) {

        String sql = """
                SELECT 1
                FROM islands
                WHERE leader_uuid = ?
                LIMIT 1
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(ownerId.toString()),
                ResultSet::next
        );
    }

    public CompletableFuture<Void> update(Island island) {
        String sql = """
                UPDATE islands
                SET
                    slot_id = ?,
                    leader_uuid = ?
                WHERE id = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                island.getSlotId(),
                                island.getLeaderId().toString(),
                                island.getIslandId()
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> delete(long islandId) {

        String sql = """
                DELETE FROM islands
                WHERE id = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    private Island mapToIsland(ResultSet rs) throws SQLException {
        return new Island(
                rs.getLong("id"),
                rs.getLong("slot_id"),
                UUID.fromString(
                        rs.getString("leader_uuid")
                )
        );
    }
}
