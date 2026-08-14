package me.connorjb05.skyblock.service.island.membership;

import me.connorjb05.skyblock.model.island.Island;

public record IslandJoinResult(
        IslandJoinStatus status,
        Island island
) {

    public static IslandJoinResult success(Island island) {
        return new IslandJoinResult(
                IslandJoinStatus.SUCCESS,
                island
        );
    }

    public static IslandJoinResult failure(IslandJoinStatus status) {
        if (status == IslandJoinStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "Failure status cannot be SUCCESS."
            );
        }

        return new IslandJoinResult(
                status,
                null
        );
    }

    public boolean successful() {
        return status == IslandJoinStatus.SUCCESS;
    }
}
