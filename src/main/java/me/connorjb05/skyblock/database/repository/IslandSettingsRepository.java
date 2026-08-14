package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.settings.IslandSetting;
import me.connorjb05.skyblock.model.island.settings.IslandSettingEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IslandSettingsRepository {

    private final Database database;

    public IslandSettingsRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<List<IslandSettingEntry>> findAll() {

        String sql = """
                SELECT
                    island_id,
                    setting,
                    value
                FROM island_settings
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(),
                resultSet -> {

                    List<IslandSettingEntry> settings = new ArrayList<>();

                    while (resultSet.next()) {
                        settings.add(mapToSetting(resultSet));
                    }

                    return settings;
                }
        );
    }

    public CompletableFuture<List<IslandSettingEntry>> findByIslandId(
            long islandId
    ) {

        String sql = """
                SELECT
                    island_id,
                    setting,
                    value
                FROM island_settings
                WHERE island_id = ?
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(islandId),
                resultSet -> {

                    List<IslandSettingEntry> settings = new ArrayList<>();

                    while (resultSet.next()) {
                        settings.add(mapToSetting(resultSet));
                    }

                    return settings;
                }
        );
    }

    public CompletableFuture<Void> setValue(
            long islandId,
            IslandSetting setting,
            boolean value
    ) {

        if (value == setting.getDefaultValue()) {
            return delete(
                    islandId,
                    setting
            );
        }

        String sql = switch (database.getType()) {

            case MYSQL -> """
                    INSERT INTO island_settings (
                        island_id,
                        setting,
                        value
                    )
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        value = VALUES(value)
                    """;

            case SQLITE -> """
                    INSERT INTO island_settings (
                        island_id,
                        setting,
                        value
                    )
                    VALUES (?, ?, ?)
                    ON CONFLICT(island_id, setting)
                    DO UPDATE SET
                        value = excluded.value
                    """;

        };

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                setting.name(),
                                value
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> delete(
            long islandId,
            IslandSetting setting
    ) {

        String sql = """
                DELETE FROM island_settings
                WHERE island_id = ?
                AND setting = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                setting.name()
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> deleteAll(long islandId) {

        String sql = """
                DELETE FROM island_settings
                WHERE island_id = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    private IslandSettingEntry mapToSetting(
            ResultSet rs
    ) throws SQLException {
        return new IslandSettingEntry(
                rs.getLong("island_id"),
                IslandSetting.valueOf(rs.getString("setting")),
                rs.getBoolean("value")
        );
    }
}
