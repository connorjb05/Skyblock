package me.connorjb05.skyblock.database;

import java.util.Locale;

public enum DatabaseType {

    SQLITE,
    MYSQL;

    public static DatabaseType fromString(String s) {

        if (s == null || s.isBlank()) {
            return SQLITE;
        }

        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatabaseException(
                    "Unknown database type: " + s
            );
        }
    }
}
