package me.connorjb05.skyblock.service.clearentities;

import me.connorjb05.skyblock.service.result.ResultStatus;

public enum ClearEntitiesStatus implements ResultStatus {

    WARNING,
    COMPLETED;

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
