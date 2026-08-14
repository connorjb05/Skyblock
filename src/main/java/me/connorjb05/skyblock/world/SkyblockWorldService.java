package me.connorjb05.skyblock.world;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.service.Service;
import org.bukkit.*;
import org.bukkit.block.Biome;

import java.util.Objects;

public class SkyblockWorldService implements Service {

    private static final String OVERWORLD_NAME = "Skyblock";
    private static final String NETHER_NAME = "Skyblock_Nether";
    private static final String END_NAME = "Skyblock_End";

    @Override
    public void onEnable(Skyblock plugin) {
        VoidChunkGenerator voidChunkGenerator = new VoidChunkGenerator();

        World overworld = Objects.requireNonNull(createVoidWorld(
                OVERWORLD_NAME,
                World.Environment.NORMAL,
                Biome.THE_VOID,
                voidChunkGenerator
        ));

        World netherWorld = Objects.requireNonNull(createVoidWorld(
                NETHER_NAME,
                World.Environment.NETHER,
                Biome.NETHER_WASTES,
                voidChunkGenerator
        ));

        World endWorld = Objects.requireNonNull(createVoidWorld(
                END_NAME,
                World.Environment.THE_END,
                Biome.THE_END,
                voidChunkGenerator
        ));

        /*
        Apply configuration settings to worlds
         */
        configureWorld(plugin, overworld);
        configureWorld(plugin, netherWorld);
        configureWorld(plugin, endWorld);
    }

    private void configureWorld(Skyblock plugin, World world) {

        world.setGameRule(
                GameRules.RANDOM_TICK_SPEED,
                plugin.getConfigService()
                        .getIslandWorldSettings()
                        .tickSpeed()
        );

        world.setDifficulty(
                plugin.getConfigService()
                        .getIslandWorldSettings()
                        .difficulty()
        );

        world.setGameRule(
                GameRules.SHOW_DEATH_MESSAGES,
                plugin.getConfigService()
                        .getIslandWorldSettings()
                        .deathMessages()
        );

        world.setGameRule(
                GameRules.SHOW_ADVANCEMENT_MESSAGES,
                false);

    }

    @Override
    public void onDisable(Skyblock plugin) {

    }

    private World createVoidWorld(
            String worldName,
            World.Environment environment,
            Biome biome,
            VoidChunkGenerator generator
    ) {
        World loadedWorld = Bukkit.getWorld(worldName);

        // World is already loaded/created
        if (loadedWorld != null) {
            return loadedWorld;
        }

        Skyblock.get().getLogger().info(
                "Generating skyblock world: " + worldName
        );

        WorldCreator creator = WorldCreator.name(worldName)
                .environment(environment)
                .generator(generator)
                .biomeProvider(new SingleBiomeProvider(biome))
                .generateStructures(false)
                .bonusChest(false);

        World world = creator.createWorld();

        if (world == null) {
            Skyblock.get().getLogger().severe(
                    "Failed to create world: " + worldName
            );

            return null;
        }

        world.setAutoSave(true);

        Skyblock.get().getLogger().info(
                "Created skyblock world: " +
                        worldName +
                        " with environment " +
                        environment
        );

        return world;
    }

    public World getOverworld() {
        return Bukkit.getWorld(OVERWORLD_NAME);
    }

    public World getNether() {
        return Bukkit.getWorld(NETHER_NAME);
    }

    public World getEnd() {
        return Bukkit.getWorld(END_NAME);
    }
}
