package me.connorjb05.skyblock.service.island.membership;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandDeclineStatus implements ResultStatus {

    SUCCESS,
    INVITE_NOT_FOUND;

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
