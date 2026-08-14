package me.connorjb05.skyblock.model.island.permission;

public record IslandPermissionEntry(
        long islandId,
        IslandPermission permission,
        int permissionLevel
) {
}
