package me.connorjb05.skyblock.model.island.membership;

public enum IslandRole {

    LEADER(4),
    CO_LEADER(3),
    MODERATOR(2),
    MEMBER(1),
    VISITOR(0);

    private final int permissionLevel;

    IslandRole(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public static IslandRole fromPermissionLevel(
            int permissionLevel
    ) {

        for (IslandRole role : values()) {
            if (role.getPermissionLevel() == permissionLevel) {
                return role;
            }
        }

        throw new IllegalArgumentException(
                "Unknown island permission level: " + permissionLevel
        );
    }

}
