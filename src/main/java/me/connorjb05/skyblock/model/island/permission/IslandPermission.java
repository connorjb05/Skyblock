package me.connorjb05.skyblock.model.island.permission;

import me.connorjb05.skyblock.model.island.membership.IslandRole;

public enum IslandPermission {

    /*
    Member permissions by default
     */
    BUILD(IslandRole.MEMBER),
    DROP_AND_PICKUP_ITEMS(IslandRole.MEMBER),
    USE_CONTAINERS(IslandRole.MEMBER),
    USE_DOORS(IslandRole.MEMBER),
    USE_REDSTONE(IslandRole.MEMBER),
    INTERACT_ENTITIES(IslandRole.MEMBER),

    /*
    Moderator permissions by default
     */
    INVITE_MEMBERS(IslandRole.CO_LEADER),
    KICK_MEMBERS(IslandRole.MODERATOR),
    BAN_VISITORS(IslandRole.MODERATOR),
    UNBAN_VISITORS(IslandRole.MODERATOR),

    /*
    Co-Leader/Leader permissions by default
     */
    MANAGE_UPGRADES(IslandRole.CO_LEADER),
    MANAGE_MEMBERS(IslandRole.CO_LEADER),
    MANAGE_PERMISSIONS(IslandRole.CO_LEADER),
    MANAGE_SETTINGS(IslandRole.CO_LEADER);


    private final IslandRole defaultRole;

    IslandPermission(IslandRole defaultRole) {
        this.defaultRole = defaultRole;
    }

    public IslandRole getDefaultRole() {
        return defaultRole;
    }

    public int getDefaultPermissionLevel() {
        return defaultRole.getPermissionLevel();
    }
}
