package me.connorjb05.skyblock;

import me.connorjb05.skyblock.command.island.IslandCommand;
import me.connorjb05.skyblock.database.Database;
import me.connorjb05.skyblock.database.DatabaseFactory;
import me.connorjb05.skyblock.listener.*;
import me.connorjb05.skyblock.menu.MenuService;
import me.connorjb05.skyblock.service.MessageService;
import me.connorjb05.skyblock.service.PlayerDataService;
import me.connorjb05.skyblock.service.ScoreboardService;
import me.connorjb05.skyblock.service.clearentities.ClearEntitiesService;
import me.connorjb05.skyblock.service.config.ConfigService;
import me.connorjb05.skyblock.service.island.IslandService;
import me.connorjb05.skyblock.world.SkyblockWorldService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Skyblock extends JavaPlugin {

    /*
    Cool island idea:

    Killing mobs/farming/building all adds
    Island XP, which can accumulate and
    make your island level higher.

    island worth will be based on
    special items placed inside of their
    island bank.
     */


    private static Skyblock instance;

    private Database database;

    private final ConfigService configService = new ConfigService();
    private final MessageService messageService = new MessageService();
    private final SkyblockWorldService skyblockWorldService = new SkyblockWorldService();
    private final IslandService islandService = new IslandService();
    private final PlayerDataService playerDataService = new PlayerDataService();


    // last thing we're worried about
    private final MenuService menuService = new MenuService();
    private final ScoreboardService scoreboardService = new ScoreboardService();
    private final ClearEntitiesService clearEntitiesService = new ClearEntitiesService();

    @Override
    public void onEnable() {
        instance = this;

        // Enable configuration service FIRST!
        configService.onEnable(this);

        // Database/Backend
        database = DatabaseFactory.create(this);

        database.connect();
        database.initialize();

        getLogger().info(
                "Successfully connected to database using " + database.getType() + "."
        );

        // Everything else

        registerServices();
        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        scoreboardService.onDisable(this);
        playerDataService.onDisable(this);
        islandService.onDisable(this);

        // Close database connection LAST
        if (database != null) {
            database.close();
        }

        // Does not matter:
        menuService.onDisable(this);
        clearEntitiesService.onDisable(this);
    }

    private void registerServices() {
        skyblockWorldService.onEnable(this);
        islandService.onEnable(this);
        playerDataService.onEnable(this);

        menuService.onEnable(this);
        scoreboardService.onEnable(this);
        clearEntitiesService.onEnable(this);
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new JoinQuitListener(this), this);

        pm.registerEvents(new IslandVoidListener(
                this,
                islandService.getAccessService()
        ), this);

        pm.registerEvents(new IslandProtectionListener(
                islandService.getAccessService()
        ), this);

        pm.registerEvents(new IslandSettingsListener(
                islandService.getAccessService(),
                islandService.getSettingsService()
        ), this);

        pm.registerEvents(new IslandUpgradeListener(
                this,
                islandService.getAccessService(),
                islandService.getUpgradeService()
        ), this);

        pm.registerEvents(new IslandBorderListener(
                this,
                islandService.getAccessService(),
                islandService.getUpgradeService(),
                islandService.getRegionProvider()
        ), this);

        pm.registerEvents(new IslandRespawnListener(
                this,
                islandService.getAccessService()
        ), this);
    }

    private void registerCommands() {
        IslandCommand islandCommand = new IslandCommand(this);

        Objects.requireNonNull(getCommand("island")).setExecutor(islandCommand);
        Objects.requireNonNull(getCommand("island")).setTabCompleter(islandCommand);
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public Database getDatabase() {
        return database;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public IslandService getIslandService() {
        return islandService;
    }

    public static Skyblock get() {
        return instance;
    }
}
