package me.connorjb05.skyblock.database.type;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.connorjb05.skyblock.database.AbstractDatabase;
import me.connorjb05.skyblock.database.DatabaseException;
import me.connorjb05.skyblock.database.DatabaseType;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SQLiteDatabase extends AbstractDatabase {

    private final File databaseFile;

    public SQLiteDatabase(
            File dataFolder,
            String fileName
    ) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {;
            throw new IllegalStateException(
                    "Failed to create database directory: " + dataFolder.getAbsolutePath()
            );
        }

        databaseFile = new File(
                dataFolder,
                fileName
        );
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    @Override
    public void connect() {

        SQLiteConfig sqliteConfig =
                new SQLiteConfig();

        sqliteConfig.setJournalMode(
                SQLiteConfig.JournalMode.WAL
        );

        sqliteConfig.setBusyTimeout(
                5_000
        );

        sqliteConfig.enforceForeignKeys(
                true
        );

        HikariConfig config =
                new HikariConfig();

        config.setPoolName(
                "Skyblock-SQLite"
        );

        config.setDriverClassName(
                "org.sqlite.JDBC"
        );

        config.setJdbcUrl(
                "jdbc:sqlite:"
                        + databaseFile.getAbsolutePath()
        );

        config.setDataSourceProperties(
                sqliteConfig.toProperties()
        );

        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);

        config.setConnectionTimeout(
                10_000
        );

        dataSource =
                new HikariDataSource(config);
    }

    @Override
    public void initialize() {

        // Islands table
        // TODO store island_level(int), island_xp(int/xp(whenver level is met reset xp), island_worth (long/balance)
        // TODO serialized home location VARCHAR(64?)
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS islands (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            
                            slot_id INTEGER NOT NULL UNIQUE
                                CHECK (slot_id >= 0),
                                
                            leader_uuid TEXT NOT NULL UNIQUE,
                            
                            home_location TEXT,
                            
                            island_level INTEGER NOT NULL DEFAULT 1
                                CHECK(island_level >= 1),
                            
                            island_xp INTEGER NOT NULL DEFAULT 0
                                CHECK(island_xp >= 0),
                            
                            island_worth INTEGER NOT NULL DEFAULT 0
                                CHECK(island_worth >= 0)
                        )
                        """,
                List.of()
        );

        // Island upgrades table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_upgrades (
                            island_id INTEGER NOT NULL,
                            upgrade TEXT NOT NULL,
                            level INTEGER NOT NULL
                                CHECK(level > 0),
                            
                            PRIMARY KEY (island_id, upgrade),
                            
                            FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        """,
                List.of()
        );

        // Island permissions table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_permissions (
                            island_id INTEGER NOT NULL,
                            permission TEXT NOT NULL,
                            permission_level INTEGER NOT NULL
                                CHECK(permission_level >= 0),
                            
                            PRIMARY KEY (island_id, permission),
                            
                            FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        """,
                List.of()
        );

        // Island settings table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_settings (
                            island_id INTEGER NOT NULL,
                            setting TEXT NOT NULL,
                            value BOOLEAN NOT NULL
                                CHECK(value IN (0, 1)),
                            
                            PRIMARY KEY (island_id, setting),
                            
                            FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        """,
                List.of()
        );

        // Island deletions queue table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_deletions (
                            island_id INTEGER NOT NULL PRIMARY KEY,
                            slot_id INTEGER NOT NULL UNIQUE
                                CHECK(slot_id >= 0),
                                
                            priority INTEGER NOT NULL DEFAULT 0,
                            queued_at INTEGER NOT NULL
                                CHECK(queued_at >= 0)
                        )
                        """,
                List.of()
        );

        // Island players table
        getSqlService().push(
                """
                    CREATE TABLE IF NOT EXISTS skyblock_players (
                        player_uuid TEXT NOT NULL PRIMARY KEY,
                        island_id INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        time_joined INTEGER NOT NULL
                            CHECK(time_joined >= 0),
                        
                        FOREIGN KEY (island_id)
                            REFERENCES islands(id)
                            ON DELETE CASCADE
                    )
                    """,
                List.of()
        );

        getSqlService().push(
                """
                    CREATE INDEX IF NOT EXISTS idx_skyblock_players_island
                    ON skyblock_players(island_id)
                    """,
                List.of()
        );
    }
}
