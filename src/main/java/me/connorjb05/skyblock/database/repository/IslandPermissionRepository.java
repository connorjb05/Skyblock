package me.connorjb05.skyblock.database.repository;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import me.connorjb05.skyblock.model.island.permission.IslandPermissionEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IslandPermissionRepository {

    private final Database database;

    public IslandPermissionRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<List<IslandPermissionEntry>> findAll() {

        String sql = """
                SELECT
                    island_id,
                    permission,
                    permission_level
                FROM island_permissions
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(),
                resultSet -> {

                    List<IslandPermissionEntry> permissions = new ArrayList<>();

                    while (resultSet.next()) {
                        permissions.add(mapToPermission(resultSet));
                    }

                    return permissions;
                }
        );
    }

    public CompletableFuture<List<IslandPermissionEntry>> findByIslandId(
            long islandId
    ) {

        String sql = """
                SELECT
                    island_id,
                    permission,
                    permission_level
                FROM island_permissions
                WHERE island_id = ?
                """;

        return database.getSqlService().pullAsync(
                sql,
                List.of(islandId),
                resultSet -> {

                    List<IslandPermissionEntry> permissions = new ArrayList<>();

                    while (resultSet.next()) {
                        permissions.add(mapToPermission(resultSet));
                    }

                    return permissions;
                }
        );
    }

    public CompletableFuture<Void> setLevel(
            long islandId,
            IslandPermission permission,
            int permissionLevel
    ) {

        if (permissionLevel < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Permission level cannot be negative."
            ));
        }

        /*
        Default permission levels are
        represented by no database row.
         */
        if (permissionLevel == permission.getDefaultPermissionLevel()) {
            return delete(
                    islandId,
                    permission
            );
        }

        String sql = switch (database.getType()) {
            case MYSQL -> """
                    INSERT INTO island_permissions (
                        island_id,
                        permission,
                        permission_level
                    )
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        permission_level = VALUES(permission_level)
                    """;

            case SQLITE -> """
                    INSERT INTO island_permissions (
                        island_id,
                        permission,
                        permission_level
                    )
                    VALUES (?, ?, ?)
                    ON CONFLICT(island_id, permission)
                    DO UPDATE SET
                        permission_level = excluded.permission_level
                    """;
        };

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                permission.name(),
                                permissionLevel
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> delete(
            long islandId,
            IslandPermission permission
    ) {

        String sql = """
                DELETE FROM island_permissions
                WHERE island_id = ?
                AND permission = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(
                                islandId,
                                permission.name()
                        )
                )
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> deleteAll(long islandId) {

        String sql = """
                DELETE FROM island_permissions
                WHERE island_id = ?
                """;

        return database.getSqlService().pushAsync(
                        sql,
                        List.of(islandId)
                )
                .thenApply(ignored -> null);
    }

    private IslandPermissionEntry mapToPermission(
            ResultSet rs
    ) throws SQLException {
        return new IslandPermissionEntry(
                rs.getLong("island_id"),
                IslandPermission.valueOf(rs.getString("permission")),
                rs.getInt("permission_level")
        );
    }
}
