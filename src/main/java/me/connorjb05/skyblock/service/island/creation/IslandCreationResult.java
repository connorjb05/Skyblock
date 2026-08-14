package me.connorjb05.skyblock.service.island.creation;

import me.connorjb05.skyblock.model.island.Island;

import java.util.Objects;

public record IslandCreationResult(
        IslandCreationStatus status,
        Island island
) {

    public IslandCreationResult {
        Objects.requireNonNull(status, "status");
    }

    public static IslandCreationResult success(Island island) {

        Objects.requireNonNull(island, "island");

        return new IslandCreationResult(
                IslandCreationStatus.SUCCESS,
                island
        );
    }

    public static IslandCreationResult failure(
            IslandCreationStatus status
    ) {

        Objects.requireNonNull(status, "status");

        if (status == IslandCreationStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "A failure result cannot use the SUCCESS status."
            );
        }

        return new IslandCreationResult(status, null);
    }

    public boolean successful() {
        return status == IslandCreationStatus.SUCCESS;
    }
}
