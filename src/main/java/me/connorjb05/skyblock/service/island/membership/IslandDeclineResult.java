package me.connorjb05.skyblock.service.island.membership;

public record IslandDeclineResult(
        IslandDeclineStatus status
) {

    public static IslandDeclineResult success() {
        return new IslandDeclineResult(IslandDeclineStatus.SUCCESS);
    }

    public static IslandDeclineResult failure(IslandDeclineStatus status) {
        return new IslandDeclineResult(status);
    }

    public boolean successful() {
        return status == IslandDeclineStatus.SUCCESS;
    }

}
