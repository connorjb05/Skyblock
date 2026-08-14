package me.connorjb05.skyblock.service.island.permission;

import me.connorjb05.skyblock.model.island.permission.IslandPermission;

public record IslandPermissionResult(
        IslandPermissionStatus status
) {

    public static IslandPermissionResult success(IslandPermissionStatus status) {
        return new IslandPermissionResult(status);
    }

    public static IslandPermissionResult failure(IslandPermissionStatus status) {
        return new IslandPermissionResult(status);
    }
}
