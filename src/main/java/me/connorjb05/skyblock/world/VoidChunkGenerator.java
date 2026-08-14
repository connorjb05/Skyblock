package me.connorjb05.skyblock.world;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class VoidChunkGenerator extends ChunkGenerator {

    private static final int SPAWN_Y = 64;

    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData
    ) {
        // Intentionally empty.
        // No blocks are placed in any generated chunk.
    }

    @Override
    public int getBaseHeight(
            WorldInfo worldInfo,
            Random random,
            int x,
            int z,
            HeightMap heightMap
    ) {
        return worldInfo.getMinHeight();
    }

    @Override
    public Location getFixedSpawnLocation(
            World world,
            Random random
    ) {
        return new Location(
                world,
                0.5,
                SPAWN_Y,
                0.5
        );
    }

    @Override
    public boolean canSpawn(
            World world,
            int x,
            int z
    ) {
        return true;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }
}
