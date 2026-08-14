package me.connorjb05.skyblock.service.config.settings;

import me.connorjb05.skyblock.model.island.upgrades.defintion.IslandSizeUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.MultiplierUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.OreGeneratorUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.SpawnerUpgradeLevel;

import java.util.NavigableMap;

public record IslandUpgradeSettings(
        NavigableMap<Integer, OreGeneratorUpgradeLevel> oreGenerator,
        NavigableMap<Integer, MultiplierUpgradeLevel> cropGrowth,
        NavigableMap<Integer, MultiplierUpgradeLevel> cropDrops,
        NavigableMap<Integer, IslandSizeUpgradeLevel> islandSize,
        NavigableMap<Integer, SpawnerUpgradeLevel> spawner,
        NavigableMap<Integer, MultiplierUpgradeLevel> mobDrops,
        NavigableMap<Integer, MultiplierUpgradeLevel> mobExperience
) {
}
