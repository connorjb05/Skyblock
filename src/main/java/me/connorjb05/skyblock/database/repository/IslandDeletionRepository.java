package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.IslandDeletion;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IslandDeletionRepository {

    private final Database database;

    public IslandDeletionRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Void> create(IslandDeletion deletion) {

        String sql = """
                INSERT INTO island_deletions (
                    island_id,
                    slot_id,
                    priority,
                    queued_at
                )
                VALUES (?, ?, ?, ?)
                """;

        return database.getSqlService()
                .pushAsync(
                        sql,
                        List.of(
                                deletion.getIslandId(),
                                deletion.getSlotId(),
                                deletion.getPriority(),
                                deletion.getQueuedAt()
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> delete(long islandId) {

        String sql = """
                DELETE FROM island_deletions
                WHERE island_id = ?
                """;

        return database.getSqlService()
                .pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<List<IslandDeletion>> findAll() {

        String sql = """
                SELECT
                    island_id,
                    slot_id,
                    priority,
                    queued_at
                FROM island_deletions
                """;

        return database.getSqlService()
                .pullAsync(
                        sql,
                        List.of(),
                        resultSet -> {

                            List<IslandDeletion> deletions = new ArrayList<>();

                            while (resultSet.next()) {
                                deletions.add(new IslandDeletion(
                                        resultSet.getLong("island_id"),
                                        resultSet.getLong("slot_id"),
                                        resultSet.getInt("priority"),
                                        resultSet.getLong("queued_at")
                                ));
                            }

                            return deletions;
                        }
                );
    }

    public CompletableFuture<Boolean> exists(long islandId) {

        String sql = """
                SELECT 1
                FROM island_deletions
                WHERE island_id = ?
                LIMIT 1
                """;

        return database.getSqlService()
                .pullAsync(
                        sql,
                        List.of(islandId),
                        ResultSet::next
                );
    }

}
