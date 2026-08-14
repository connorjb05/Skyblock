package me.connorjb05.skyblock.service.island.membership;

public record IslandLeaveResult(
        IslandLeaveStatus status
) {

    public static IslandLeaveResult success() {
        return new IslandLeaveResult(IslandLeaveStatus.SUCCESS);
    }

    public static IslandLeaveResult failure(IslandLeaveStatus status) {
        if (status == IslandLeaveStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Failure status cannot be SUCCESS."
            );
        }

        return new IslandLeaveResult(status);
    }

    public boolean successful() {
        return status == IslandLeaveStatus.SUCCESS;
    }
}
