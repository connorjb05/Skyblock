package me.connorjb05.skyblock.database;

import me.connorjb05.skyblock.service.sql.SqlService;

import java.sql.Connection;
import java.sql.SQLException;

public interface Database extends AutoCloseable {
    SqlService getSqlService();
    DatabaseType getType();
    void connect();
    void initialize();
    Connection getConnection() throws SQLException;
    boolean isConnected();
    @Override
    void close();
}
