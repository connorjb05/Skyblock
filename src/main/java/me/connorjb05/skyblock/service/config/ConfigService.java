package me.connorjb05.skyblock.service.config;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.model.island.upgrades.defintion.*;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.service.clearentities.ClearEntitiesStatus;
import me.connorjb05.skyblock.service.config.scoreboard.ConfigBoard;
import me.connorjb05.skyblock.service.config.settings.*;
import me.connorjb05.skyblock.service.island.creation.IslandCreationStatus;
import me.connorjb05.skyblock.service.island.deletion.IslandDeletionStatus;
import me.connorjb05.skyblock.service.island.membership.IslandDeclineStatus;
import me.connorjb05.skyblock.service.island.membership.IslandJoinStatus;
import me.connorjb05.skyblock.service.island.membership.IslandLeaveStatus;
import me.connorjb05.skyblock.service.island.permission.IslandPermissionStatus;
import me.connorjb05.skyblock.service.island.teleport.IslandTeleportStatus;
import me.connorjb05.skyblock.service.result.ResultStatus;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigService implements Service {

    private FileConfiguration settings;
    private FileConfiguration upgrades;
    private FileConfiguration messages;
    private FileConfiguration scoreboard;

    private IslandWorldSettings islandWorldSettings;
    private IslandRegionSettings islandRegionSettings;
    private IslandInviteSettings islandInviteSettings;
    private IslandUpgradeSettings islandUpgradeSettings;

    private ClearEntitiesSettings clearEntitiesSettings;
    private MobStackerSettings mobStackerSettings;
    private ItemStackerSettings itemStackerSettings;

    private ConfigBoard loadingBoard;
    private ConfigBoard noIslandBoard;
    private ConfigBoard islandBoard;

    @Override
    public void onEnable(
            Skyblock plugin
    ) {

        /*
         * config.yml
         *
         * Database/backend configuration.
         */
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        /*
        Skyblock island upgrades configuration.
         */
        loadIslandUpgrades(plugin);

        /*
         * Skyblock settings specific configuration.
         */
        loadSettings(plugin);

        /*
         * Player-facing messages.
         */
        loadMessages(plugin);

        /*
         * Scoreboards
         */
        loadScoreboard(plugin);
    }

    @Override
    public void onDisable(
            Skyblock plugin
    ) {

    }

    private void loadScoreboard(Skyblock plugin) {
        scoreboard = loadConfig(plugin, "scoreboard.yml");

        loadingBoard = new ConfigBoard(
                scoreboard.getString("loading.title"),
                scoreboard.getStringList("loading.lines")
        );

        noIslandBoard = new ConfigBoard(
                scoreboard.getString("no_island.title"),
                scoreboard.getStringList("no_island.lines")
        );

        islandBoard = new ConfigBoard(
                scoreboard.getString("island.title"),
                scoreboard.getStringList("island.lines")
        );
    }

    private void loadSettings(
            Skyblock plugin
    ) {

        settings = loadConfig(plugin, "settings.yml");

        loadIslandWorldSettings();
        loadIslandRegionSettings();
        loadIslandInviteSettings();
        loadClearEntitiesSettings();
        loadStackerSettings();
    }

    private void loadIslandWorldSettings() {
        String path = "islands.world.";

        String configDifficulty = settings
                .getString(path + "difficulty", "EASY");

        Difficulty difficulty;

        try {
            difficulty = Difficulty.valueOf(configDifficulty);
        } catch (Exception ignored) {
            difficulty = Difficulty.EASY;
        }

        islandWorldSettings = new IslandWorldSettings(
                settings.getInt(
                        path + "tick_speed",
                        8
                ),
                difficulty,
                settings.getBoolean(
                        path + "death_messages",
                        true
                )
        );
    }

    private void loadIslandUpgrades(Skyblock plugin) {

        upgrades = loadConfig(plugin, "upgrades.yml");

        islandUpgradeSettings = new IslandUpgradeSettings(
                loadGeneratorLevels(),
                loadMultiplierLevels("crop_growth"),
                loadMultiplierLevels("crop_drops"),
                loadIslandSizeLevels(),
                loadSpawnerLevels(),
                loadMultiplierLevels("mob_drops"),
                loadMultiplierLevels("mob_experience")
        );
    }

    private NavigableMap<Integer, SpawnerUpgradeLevel> loadSpawnerLevels() {

        String path = "spawner";

        ConfigurationSection section = upgrades.getConfigurationSection(path);

        if (section == null) {
            throw new IllegalStateException(
                    "Missing upgrade section: " + path
            );
        }

        NavigableMap<Integer, SpawnerUpgradeLevel> levels = new TreeMap<>();

        for (String key : section.getKeys(false)) {

            int level;

            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid upgrade level: " + path + "." + key
                );
            }

            String levelPath = path + "." + level + ".";

            levels.put(level, new SpawnerUpgradeLevel(
                    level,
                    upgrades.getDouble(
                            levelPath + "cost"
                    ),
                    upgrades.getDouble(
                            levelPath + "speed_multiplier",
                            1.0D
                    ),
                    upgrades.getDouble(
                            levelPath + "amount_multiplier",
                            1.0D
                    ),
                    upgrades.getDouble(
                            levelPath + "chance",
                            100.0D
                    )
            ));
        }

        validateLevels(
                path,
                levels
        );

        return levels;
    }

    private NavigableMap<Integer, IslandSizeUpgradeLevel> loadIslandSizeLevels() {

        String path = "island_size";

        ConfigurationSection section = upgrades.getConfigurationSection(path);

        if (section == null) {
            throw new IllegalStateException(
                    "Missing upgrade section: " + path
            );
        }

        NavigableMap<Integer, IslandSizeUpgradeLevel> levels = new TreeMap<>();

        for (String key : section.getKeys(false)) {

            int level = Integer.parseInt(key);

            String levelPath = path + "." + level + ".";

            levels.put(level, new IslandSizeUpgradeLevel(
                    level,
                    upgrades.getDouble(
                            levelPath + "cost"
                    ),
                    upgrades.getInt(
                            levelPath + "size"
                    )
            ));
        }

        validateLevels(
                path,
                levels
        );

        return levels;
    }

    private NavigableMap<Integer, MultiplierUpgradeLevel> loadMultiplierLevels(
            String path
    ) {

        ConfigurationSection section =
                upgrades.getConfigurationSection(path);

        if (section == null) {
            throw new IllegalStateException(
                    "Missing upgrade section: " + path
            );
        }

        NavigableMap<Integer, MultiplierUpgradeLevel> levels =
                new TreeMap<>();

        for (String key : section.getKeys(false)) {

            int level;

            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid upgrade level: "
                                + path
                                + "."
                                + key
                );
            }

            String levelPath =
                    path
                            + "."
                            + level
                            + ".";

            double multiplier =
                    upgrades.getDouble(
                            levelPath + "multiplier",
                            1.0D
                    );

            double chance =
                    upgrades.getDouble(
                            levelPath + "chance",
                            100.0D
                    );

            levels.put(level,
                    new MultiplierUpgradeLevel(
                            level,
                            upgrades.getDouble(
                                    levelPath + "cost"
                            ),
                            multiplier,
                            chance
                    ));
        }

        validateLevels(
                path,
                levels
        );

        return levels;
    }

    private NavigableMap<Integer, OreGeneratorUpgradeLevel> loadGeneratorLevels() {

        String path = "ore_generator";

        ConfigurationSection section = upgrades.getConfigurationSection(path);

        if (section == null) {
            throw new IllegalStateException(
                    "Missing upgrade section: " + path
            );
        }

        NavigableMap<Integer, OreGeneratorUpgradeLevel> levels = new TreeMap<>();

        for (String key : section.getKeys(false)) {

            int level = Integer.parseInt(key);

            String levelPath = path + "." + level;

            ConfigurationSection materialSection = upgrades.getConfigurationSection(
                    levelPath + ".materials"
            );

            if (materialSection == null) {
                throw new IllegalStateException(
                        "Missing materials for " + levelPath
                );
            }

            List<GeneratorMaterial> materials = new ArrayList<>();

            double totalChance = 0.0D;

            for (String materialName : materialSection.getKeys(false)) {

                Material material = Material.matchMaterial(materialName);

                if (material == null) {
                    throw new IllegalStateException(
                            "Invalid ore generator material: " + materialName
                    );
                }

                double chance = materialSection.getDouble(materialName + ".chance");

                if (chance < 0.0D || chance > 100.0D) {
                    throw new IllegalStateException(
                            "Invalid ore generator chance for " + materialName
                    );
                }

                materials.add(new GeneratorMaterial(material, chance));
                totalChance += chance;
            }

            if (Math.abs(totalChance - 100.0D) > 0.0001D) {
                throw new IllegalStateException(
                        levelPath + " material chances total " +
                                totalChance + "% instead of 100%."
                );
            }

            levels.put(level, new OreGeneratorUpgradeLevel(
                    level,
                    upgrades.getDouble(
                            levelPath + ".cost"
                    ),
                    List.copyOf(materials)
            ));
        }

        validateLevels(
                path,
                levels
        );

        return levels;
    }

    private void validateLevels(String name, NavigableMap<Integer, ?> levels) {

        if (levels.isEmpty()) {
            throw new IllegalStateException(
                    name + " has no configured levels."
            );
        }

        if (!levels.containsKey(0)) {
            throw new IllegalStateException(
                    name + " must contain level 0."
            );
        }

        int expectedLevel = 0;

        for (int level : levels.keySet()) {

            if (level != expectedLevel) {
                throw new IllegalStateException(
                        name + " is missing level " + expectedLevel + "."
                );
            }

            expectedLevel++;
        }

    }

    private void loadStackerSettings() {

        String mobStackerPath = "islands.mob_stacker.";

        mobStackerSettings = new MobStackerSettings(
                settings.getBoolean(
                        mobStackerPath + "enabled",
                        true
                ),
                settings.getDouble(
                        mobStackerPath + "radius",
                        8.0
                ),
                settings.getDouble(
                        mobStackerPath + "y_radius",
                        4.0
                ),
                settings.getString(
                        mobStackerPath + "name",
                        "<gold><bold>x<amount></bold> <white><mob>"
                )
        );

        String itemStackerPath = "islands.item_stacker.";

        itemStackerSettings = new ItemStackerSettings(
                settings.getBoolean(
                        itemStackerPath + "enabled",
                        true
                ),
                settings.getDouble(
                        itemStackerPath + "radius",
                        8.0
                ),
                settings.getDouble(
                        itemStackerPath + "y_radius",
                        4.0
                ),
                settings.getString(
                        itemStackerPath + "name",
                        "<gold><bold>x<amount></bold> <white><item>"
                )
        );
    }

    private void loadClearEntitiesSettings() {
        clearEntitiesSettings = new ClearEntitiesSettings(
                settings.getBoolean(
                        "clear_entities.enabled",
                        true
                ),
                settings.getInt(
                        "clear_entities.interval",
                        300
                )
        );
    }

    private void loadIslandRegionSettings() {

        islandRegionSettings =
                new IslandRegionSettings(

                        settings.getString(
                                "islands.region.world",
                                "Skyblock"
                        ),

                        settings.getInt(
                                "islands.region.size",
                                200
                        ),

                        settings.getInt(
                                "islands.region.spacing",
                                500
                        ),

                        settings.getInt(
                                "islands.region.slots-per-row",
                                1000
                        ),

                        settings.getInt(
                                "islands.region.origin-x",
                                0
                        ),

                        settings.getInt(
                                "islands.region.origin-z",
                                0
                        )
                );
    }

    private void loadIslandInviteSettings() {

        long durationSeconds =
                settings.getLong(
                        "islands.invitations.duration-seconds",
                        60L
                );

        /*
         * Convert once while loading.
         *
         * All runtime code can now work
         * directly with milliseconds.
         */
        long durationMillis = durationSeconds * 1000L;

        islandInviteSettings = new IslandInviteSettings(durationMillis);
    }

    private void loadMessages(
            Skyblock plugin
    ) {

        messages = loadConfig(plugin, "messages.yml");

        importMessages(
                messages,
                "island_command",
                IslandCommandStatus.class
        );

        importMessages(
                messages,
                "island_creation",
                IslandCreationStatus.class
        );

        importMessages(
                messages,
                "island_deletion",
                IslandDeletionStatus.class
        );

        importMessages(
                messages,
                "island_permission",
                IslandPermissionStatus.class
        );

        importMessages(
                messages,
                "decline_island_invitation",
                IslandDeclineStatus.class
        );

        importMessages(
                messages,
                "island_teleport",
                IslandTeleportStatus.class
        );

        importMessages(
                messages,
                "join_island",
                IslandJoinStatus.class
        );

        importMessages(
                messages,
                "leave_island",
                IslandLeaveStatus.class
        );

        importMessages(
                messages,
                "clear_entities",
                ClearEntitiesStatus.class
        );
    }

    private <E extends Enum<E> & ResultStatus> void importMessages(
            FileConfiguration configuration,
            String root,
            Class<E> enumClass
    ) {

        for (E status : enumClass.getEnumConstants()) {

            String path = root + "." + status.name().toLowerCase(Locale.ROOT);
            String message = configuration.getString(path);

            if (message == null) {
                throw new IllegalStateException(
                        "Missing message configuration: " + path
                );
            }

            status.setMessage(message);
        }
    }

    private FileConfiguration loadConfig(
            Skyblock plugin,
            String name
    ) {

        File file =
                new File(
                        plugin.getDataFolder(),
                        name
                );

        if (!file.exists()) {
            plugin.saveResource(
                    name,
                    false
            );
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public ConfigBoard getLoadingBoard() {
        return loadingBoard;
    }

    public ConfigBoard getNoIslandBoard() {
        return noIslandBoard;
    }

    public ConfigBoard getIslandBoard() {
        return islandBoard;
    }

    public IslandWorldSettings getIslandWorldSettings() {
        return islandWorldSettings;
    }

    public IslandRegionSettings getIslandRegionSettings() {
        return islandRegionSettings;
    }

    public IslandUpgradeSettings getIslandUpgradeSettings() {
        return islandUpgradeSettings;
    }

    public IslandInviteSettings getIslandInviteSettings() {
        return islandInviteSettings;
    }

    public ClearEntitiesSettings getClearEntitiesSettings() {
        return clearEntitiesSettings;
    }

    public MobStackerSettings getMobStackerSettings() {
        return mobStackerSettings;
    }

    public ItemStackerSettings getItemStackerSettings() {
        return itemStackerSettings;
    }

    public FileConfiguration getMessagesFile() {
        return messages;
    }
}