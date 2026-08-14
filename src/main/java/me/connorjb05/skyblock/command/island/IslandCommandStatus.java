package me.connorjb05.skyblock.command.island;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum IslandCommandStatus implements ResultStatus {

    PLAYER_DATA_LOADING,

    NOT_IN_ISLAND,
    ISLAND_NOT_FOUND,

    USAGE,
    INVALID_ISLAND_ID,

    PLAYER_NOT_ONLINE,
    TARGET_DATA_LOADING,
    TARGET_ALREADY_IN_ISLAND,
    CANNOT_TARGET_SELF,

    INVITE_ALREADY_PENDING,
    INVITE_SENT,
    INVITE_RECEIVED,

    CREATE_FAILURE,
    TELEPORT_FAILURE,
    DELETE_FAILURE,
    JOIN_FAILURE,
    LEAVE_FAILURE,

    INVALID_PERMISSION,
    INVALID_ROLE;

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
