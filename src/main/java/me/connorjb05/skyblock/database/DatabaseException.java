package me.connorjb05.skyblock.database;

public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
