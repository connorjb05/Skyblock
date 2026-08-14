package me.connorjb05.skyblock.model.island.permission;

import me.connorjb05.skyblock.model.island.membership.IslandRole;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class IslandPermissionData {

    /*
    Contains only permission levels that
    differ from their default values.
     */
    private final EnumMap<IslandPermission, Integer> permissions = new EnumMap<>(
            IslandPermission.class
    );

    /*
    Gets the effective permission level.

    If the island has not customized this
    permission, its default level is used
     */
    public int getPermissionLevel(IslandPermission permission) {
        return permissions.getOrDefault(
                permission,
                permission.getDefaultPermissionLevel()
        );
    }

    /*
    Sets a custom permission level.

    If the supplied level is the default,
    the override is removed.
     */
    public void setPermissionLevel(
            IslandPermission permission,
            int permissionLevel
    ) {

        validatePermissionLevel(permissionLevel);

        if (permissionLevel == permission.getDefaultPermissionLevel()) {
            permissions.remove(permission);
            return;
        }

        permissions.put(
                permission,
                permissionLevel
        );
    }

    /*
    Resets a permission back
    to its default level.
     */
    public void resetPermission(IslandPermission permission) {
        permissions.remove(permission);
    }

    /*
    Returns whether this island has
    customized a permission.
     */
    public boolean hasOverride(IslandPermission permission) {
        return permissions.containsKey(permission);
    }

    /*
    Returns the island specific
    permission overrides.
     */
    public Map<IslandPermission, Integer> getOverrides() {
        return Collections.unmodifiableMap(permissions);
    }

    /*
    Returns every permission
    with effective permission level.
     */
    public Map<IslandPermission, Integer> getPermissions() {

        EnumMap<IslandPermission, Integer> effectivePermissions = new EnumMap<>(
                IslandPermission.class
        );

        for (IslandPermission permission : IslandPermission.values()) {
            effectivePermissions.put(
                    permission,
                    getPermissionLevel(permission)
            );
        }

        return Collections.unmodifiableMap(effectivePermissions);
    }

    /*
    Clears all island specific
    permission overrides
     */
    public void reset() {
        permissions.clear();
    }

    private void validatePermissionLevel(int permissionLevel) {
        if (permissionLevel < IslandRole.VISITOR.getPermissionLevel()
                || permissionLevel > IslandRole.LEADER.getPermissionLevel()) {
            throw new IllegalArgumentException(
                    "Invalid island permission level: " + permissionLevel
            );
        }
    }
}
