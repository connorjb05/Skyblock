package me.connorjb05.skyblock.service.island.deletion;

import me.connorjb05.skyblock.model.island.IslandDeletion;

public record IslandDeletionResult(
        IslandDeletionStatus status,
        IslandDeletion deletion
) {

    public static IslandDeletionResult queued(IslandDeletion deletion) {
        return new IslandDeletionResult(
                IslandDeletionStatus.QUEUED,
                deletion
        );
    }

    public static IslandDeletionResult failure(IslandDeletionStatus status) {
        if (status == IslandDeletionStatus.QUEUED) {
            throw new IllegalArgumentException(
                    "Failure cannot use QUEUED status."
            );
        }

        return new IslandDeletionResult(
                status,
                null
        );
    }

    public boolean successful() {
        return status == IslandDeletionStatus.QUEUED;
    }
}
