package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeEntry;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IslandUpgradeRepository {

    private final Database database;

    public IslandUpgradeRepository(Database database) {
        this.database = database;
    }

    /*
     * Finds every upgrade that has
     * actually been purchased.
     *
     * Level 0 upgrades do not have database rows.
     */
    public CompletableFuture<List<IslandUpgradeEntry>> findAll() {

        String sql = """
                SELECT
                    island_id,
                    upgrade,
                    level
                FROM island_upgrades
                WHERE level > 0
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(),
                resultSet -> {

                    List<IslandUpgradeEntry> upgrades = new ArrayList<>();

                    while (resultSet.next()) {
                        upgrades.add(mapToUpgrade(resultSet));
                    }

                    return upgrades;
                }
        );
    }

    /*
    Finds all purchased upgrades
    belonging to one island.
     */
    public CompletableFuture<List<IslandUpgradeEntry>> findByIslandId(
            long islandId
    ) {

        String sql = """
                SELECT
                    island_id,
                    upgrade,
                    level
                FROM island_upgrades
                WHERE island_id = ?
                AND level > 0
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(islandId),
                resultSet -> {

                    List<IslandUpgradeEntry> upgrades = new ArrayList<>();

                    while (resultSet.next()) {
                        upgrades.add(mapToUpgrade(resultSet));
                    }

                    return upgrades;
                }
        );
    }

    /*
    Persists an upgrade level.

    Level 0 is NEVER stored in the database.
     */
    public CompletableFuture<Void> setLevel(
            long islandId,
            IslandUpgradeType type,
            int level
    ) {

        if (level < 0) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Upgrade level cannot be negative."
                    )
            );
        }

        /*
        Level 0 is represented by
        no database row.
         */
        if (level == 0) {
            return delete(
                    islandId,
                    type
            );
        }

        String sql =
                switch (database.getType()) {

                    case MYSQL -> """
                            INSERT INTO island_upgrades (
                                island_id,
                                upgrade,
                                level
                            )
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                level = VALUES(level)
                            """;

                    case SQLITE -> """
                            INSERT INTO island_upgrades (
                                island_id,
                                upgrade,
                                level
                            )
                            VALUES (?, ?, ?)
                            ON CONFLICT(island_id, upgrade)
                            DO UPDATE SET
                                level = excluded.level
                            """;
                };

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                type.name(),
                                level
                        )
                )
                .thenApply(ignored -> null);
    }

    /*
    Removes one upgrade row.

    This basically returns that
    upgrade to level 0.
     */
    public CompletableFuture<Void> delete(
            long islandId,
            IslandUpgradeType type
    ) {

        String sql = """
                DELETE FROM island_upgrades
                WHERE island_id = ?
                AND upgrade = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                type.name()
                        )
                )
                .thenApply(ignored -> null);
    }

    /*
    Removes every stored upgrade
    belonging to an island.
     */
    public CompletableFuture<Void> deleteAll(long islandId) {

        String sql = """
                DELETE FROM island_upgrades
                WHERE island_id = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    private IslandUpgradeEntry mapToUpgrade(
            ResultSet rs
    ) throws SQLException {
        return new IslandUpgradeEntry(
                rs.getLong("island_id"),
                IslandUpgradeType.valueOf(rs.getString("upgrade")),
                rs.getInt("level")
        );
    }
}
