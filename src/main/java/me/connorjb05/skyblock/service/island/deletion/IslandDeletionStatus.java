package me.connorjb05.skyblock.service.island.deletion;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandDeletionStatus implements ResultStatus {
    QUEUED,
    NOT_IN_ISLAND,
    NOT_OWNER,
    ALREADY_QUEUED;

    private String message;

    @Override
    public void setMessage(
            String message
    ) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
