package me.connorjb05.skyblock.database;

import com.zaxxer.hikari.HikariDataSource;
import me.connorjb05.skyblock.service.sql.SqlService;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabase implements Database {

    protected HikariDataSource dataSource;

    private final SqlService sqlService;

    protected AbstractDatabase() {
        this.sqlService = new SqlService(this);
    }

    @Override
    public SqlService getSqlService() {
        return sqlService;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException(
                    "Database is not connected."
            );
        }

        return dataSource.getConnection();
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public void close() {
        if (dataSource == null) {
            return;
        }

        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
