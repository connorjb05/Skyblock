package me.connorjb05.skyblock.database;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.type.MySQLDatabase;
import me.connorjb05.skyblock.database.type.SQLiteDatabase;
import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseFactory {

    private DatabaseFactory() {}

    public static Database create(
            Skyblock plugin
    ) {
        FileConfiguration config = plugin.getConfig();

        DatabaseType type = DatabaseType.fromString(
                config.getString(
                        "database.type",
                        "SQLITE"
                )
        );

        return switch (type) {
            case SQLITE -> createSQLite(
                    plugin,
                    config
            );

            case MYSQL -> createMySQL(
                    config
            );
        };
    }

    private static Database createSQLite(
            Skyblock plugin,
            FileConfiguration config
    ) {
        String fileName = config.getString(
                "database.sqlite.file",
                "skyblock.db"
        );

        return new SQLiteDatabase(
                plugin.getDataFolder(),
                fileName
        );
    }

    private static Database createMySQL(
            FileConfiguration config
    ) {
        String host = config.getString(
                "database.mysql.host",
                "localhost"
        );

        int port = config.getInt(
                "database.mysql.port",
                3306
        );

        String database = config.getString(
                "database.mysql.database",
                "skyblock"
        );

        String username = config.getString(
                "database.mysql.username",
                "root"
        );

        String password = config.getString(
                "database.mysql.password",
                "password"
        );

        int poolSize = config.getInt(
                "database.mysql.pool-size",
                10
        );

        boolean useSsl = config.getBoolean(
                "database.mysql.use-ssl",
                false
        );

        return new MySQLDatabase(
                host,
                port,
                database,
                username,
                password,
                poolSize,
                useSsl
        );
    }
}
