package me.connorjb05.skyblock.service.sql;

import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.database.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service utility class for cleanly and efficiently
 * querying and writing to the sql database.
 *
 * This class is completely database/table independent.
 */
public class SqlService {

    private final Database database;

    /**
     * Creates a new SQL service for the provided database.
     *
     * @param database
     */
    public SqlService(Database database) {
        this.database = database;
    }

    /**
     * Pulls data asynchronously from the database.
     *
     * @param sql        SQL query
     * @param parameters query parameters
     * @param mapper     result mapper
     * @param <T>        resulting type
     * @return future containing the mapped result
     */
    public <T> CompletableFuture<T> pullAsync(
            String sql,
            List<?> parameters,
            SqlMapper<T> mapper
    ) {
        return CompletableFuture.supplyAsync(() -> pull(sql, parameters, mapper));
    }

    /**
     * Pulls data synchronously from the database.
     *
     * @param sql        SQL query
     * @param parameters query parameters
     * @param mapper     result mapper
     * @param <T>        resulting type
     * @return mapped result
     */
    public <T> T pull(
            String sql,
            List<?> parameters,
            SqlMapper<T> mapper
    ) {
        try (
                Connection connection = database.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
        ) {

            applyParameters(ps, parameters);

            try (
                    ResultSet resultSet = ps.executeQuery();
            ) {
                return mapper.map(resultSet);
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to pull from the database.",
                    e
            );
        }
    }

    /**
     * Pushes data asynchronously to the database.
     *
     * @param sql SQL statement
     * @param parameters statement parameters
     * @return future containing the number of affected rows
     */
    public CompletableFuture<Integer> pushAsync(
            String sql,
            List<Object> parameters
    ) {
        return CompletableFuture.supplyAsync(() -> push(sql, parameters));
    }

    /**
     * Pushes data synchronously to the database.
     *
     * Can be used for INSERT, UPDATE, and DELETE statements.
     *
     * @param sql SQL statement
     * @param parameters statement parameters
     * @return number of affected rows
     */
    public int push(
            String sql,
            List<Object> parameters
    ) {
        try (
                Connection connection = database.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            applyParameters(ps, parameters);

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to push synchronously to database.",
                    e
            );
        }
    }

    /**
     * Pushes data asynchronously and returns
     * the generated database key.
     *
     * Will be used for AUTO_INCREMENT Ids
     *
     * @param sql SQL query
     * @param parameters statement parameters
     * @return future containing generated ID
     */
    public CompletableFuture<Long> pushKeyAsync(
            String sql,
            List<?> parameters
    ) {
        return CompletableFuture.supplyAsync(() -> pushKey(sql, parameters));
    }

    /**
     * Pushes data synchronously and returns the generated database key.
     *
     * Used for AUTO_INCREMENT Ids
     *
     * @param sql SQL statement
     * @param parameters statement parameters
     * @return generated ID
     */
    public long pushKey(
            String sql,
            List<?> parameters
    ) {
        try (
                Connection connection = database.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        sql,
                        PreparedStatement.RETURN_GENERATED_KEYS
                )
        ) {
            applyParameters(ps, parameters);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DatabaseException(
                        "Failed to push to database. No rows were affected."
                );
            }

            try (
                    ResultSet resultSet = ps.getGeneratedKeys()
            ) {
                if (!resultSet.next()) {
                    throw new DatabaseException(
                            "Failed to retrieve generated database key."
                    );
                }

                return resultSet.getLong(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to push to the database.",
                    e
            );
        }
    }

    /**
     * Applies parameters to a prepared statement.
     *
     * @param ps prepared statement
     * @param parameters parameters to apply
     */
    private void applyParameters(
            PreparedStatement ps,
            List<?> parameters
    ) throws SQLException {

        if (parameters == null) {
            return;
        }

        for (
                int i = 0;
                i < parameters.size();
                i++
        ) {
            ps.setObject(i + 1, parameters.get(i));
        }
    }
}
