package me.connorjb05.skyblock.service.island.membership;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandLeaveStatus implements ResultStatus {

    SUCCESS,
    NOT_IN_ISLAND,
    LEADER_CANNOT_LEAVE;

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

