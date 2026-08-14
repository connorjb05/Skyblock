package me.connorjb05.skyblock.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingleBiomeProvider extends BiomeProvider {

    private final Biome biome;
    private final List<Biome> biomes;

    public SingleBiomeProvider(Biome biome) {
        this.biome = biome;
        this.biomes = List.of(biome);
    }

    @Override
    public @NotNull Biome getBiome(
            @NotNull WorldInfo worldInfo,
            int x,
            int y,
            int z
    ) {
        return biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(
            @NotNull WorldInfo worldInfo
    ) {
        return biomes;
    }
}
