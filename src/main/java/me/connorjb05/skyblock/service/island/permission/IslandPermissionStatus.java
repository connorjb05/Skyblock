package me.connorjb05.skyblock.service.island.permission;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandPermissionStatus implements ResultStatus {

    UPDATE_SUCCESS,
    RESET_SUCCESS,
    RESET_ALL_SUCCESS,

    NO_PERMISSION,
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
