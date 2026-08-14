package me.connorjb05.skyblock.database.type;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.connorjb05.skyblock.database.AbstractDatabase;
import me.connorjb05.skyblock.database.DatabaseType;

import java.util.List;

public class MySQLDatabase extends AbstractDatabase {

    private final String host;
    private final int port;
    private final String database;

    private final String username;
    private final String password;

    private final int poolSize;
    private final boolean useSsl;

    public MySQLDatabase(
            String host,
            int port,
            String database,
            String username,
            String password,
            int poolSize,
            boolean useSsl
    ) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.useSsl = useSsl;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();

        config.setPoolName("Skyblock-MySQL");

        config.setDriverClassName(
                "com.mysql.cj.jdbc.Driver"
        );

        String jdbcUrl =
                "jdbc:mysql://"
                        + host
                        + ":"
                        + port
                        + "/"
                        + database
                        + "?useSSL="
                        + useSsl
                        + "&serverTimezone=UTC"
                        + "&allowPublicKeyRetrieval=true";

        config.setJdbcUrl(jdbcUrl);

        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(poolSize);

        config.setMinimumIdle(
                Math.min(
                        2,
                        poolSize
                )
        );

        // Max time code may wait for connection
        config.setConnectionTimeout(10_000);

        // validate connections quickly if database becomes unreachable.
        config.setValidationTimeout(5_000);

        // Extra idle connections can be removed after 10 min
        config.setIdleTimeout(600_000);

        // Recycle connections periodically instead of keeping the same
        // connection forever.
        config.setMaxLifetime(1_800_000);

        // Periodically validate idle connections.
        config.setKeepaliveTime(120_000);

        dataSource = new HikariDataSource(config);
    }

    @Override
    public void initialize() {

        // Islands table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS islands (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            
                            slot_id BIGINT NOT NULL,
                            
                            leader_uuid CHAR(36)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                            
                            home_location VARCHAR(255),
                            
                            island_level INT NOT NULL DEFAULT 1,
                            island_xp INT NOT NULL DEFAULT 0,
                            island_worth BIGINT NOT NULL DEFAULT 0,
                            
                            PRIMARY KEY (id),
                            
                            UNIQUE_KEY uk_islands_slot_id (
                                slot_id
                            ),
                            
                            UNIQUE KEY uk_islands_leader_uuid (
                                leader_uuid
                            ),
                            
                            CONSTRAINT chk_islands_slot_id
                                CHECK (slot_id >= 0),
                            
                            CONSTRAINT chk_islands_level
                                CHECK (island_level >= 1),
                            
                            CONSTRAINT chk_islands_xp
                                CHECK (island_xp >= 0),
                            
                            CONSTRAINT chk_island_worth
                                CHECK (island_worth >= 0)
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );

        // Island upgrades table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_upgrades (
                            island_id BIGINT NOT NULL,
                            
                            upgrade VARCHAR(32)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                                
                            level INT NOT NULL,
                            
                            PRIMARY KEY (island_id, upgrade),
                            
                            CONSTRAINT chk_island_upgrades_level
                                CHECK (level > 0),
                            
                            CONSTRAINT fk_island_upgrades_island
                                FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );

        // Island permissions table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_permissions (
                            island_id BIGINT NOT NULL,
                            
                            permission VARCHAR(64)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                            
                            permission_level INT NOT NULL,
                            
                            PRIMARY KEY (island_id, permission),
                            
                            CONSTRAINT chk_island_permissions_level
                                CHECK (permission_level >= 0),
                            
                            CONSTRAINT fk_island_permissions_island_id
                                FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );

        // Island settings table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_settings (
                            island_id BIGINT NOT NULL,
                            
                            setting VARCHAR(32)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                            
                            value TINYINT(1) NOT NULL,
                            
                            PRIMARY KEY (island_id, setting),
                            
                            CONSTRAINT chk_island_settings_value
                                CHECK (value IN (0, 1)),
                            
                            CONSTRAINT fk_island_settings_island_id
                                FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );

        // Island deletions queue table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS island_deletions (
                            island_id BIGINT NOT NULL,
                            slot_id BIGINT NOT NULL,
                            
                            priority INT NOT NULL DEFAULT 0,
                            queued_at BIGINT NOT NULL,
                            
                            PRIMARY KEY (island_id),
                            
                            UNIQUE KEY uk_island_deletions_slot (slot_id),
                            
                            CONSTRAINT chk_island_deletions_slot
                                CHECK (slot_id >= 0),
                            
                            CONSTRAINT chk_island_deletions_queued_at
                                CHECK (queued_at >= 0)
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );

        // Island players table
        getSqlService().push(
                """
                        CREATE TABLE IF NOT EXISTS skyblock_players (
                            player_uuid CHAR(36)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                            
                            island_id BIGINT NOT NULL,
                            
                            role VARCHAR(32)
                                CHARACTER SET ascii
                                COLLATE ascii_bin
                                NOT NULL,
                            
                            time_joined BIGINT NOT NULL,
                            
                            PRIMARY KEY (player_uuid),
                            
                            KEY idx_skyblock_players_island (
                                island_id
                            ),
                            
                            CONSTRAINT chk_skyblock_players_time_joined
                                CHECK (time_joined >= 0),
                            
                            CONSTRAINT fk_skyblock_players_island
                                FOREIGN KEY (island_id)
                                REFERENCES islands(id)
                                ON DELETE CASCADE
                        )
                        ENGINE=InnoDB
                        DEFAULT CHARACTER SET=utf8mb4
                        COLLATE=utf8mb4_0900_ai_ci
                        """,
                List.of()
        );
    }
}
