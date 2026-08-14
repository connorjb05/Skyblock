package me.connorjb05.skyblock.service.island.teleport;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandTeleportStatus implements ResultStatus {

    SUCCESS,
    TITLE,
    SUBTITLE,
    FAILURE;

    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }
}
