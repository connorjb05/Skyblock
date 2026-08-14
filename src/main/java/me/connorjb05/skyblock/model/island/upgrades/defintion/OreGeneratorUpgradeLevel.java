package me.connorjb05.skyblock.model.island.upgrades.defintion;

import java.util.List;

public record OreGeneratorUpgradeLevel(
        int level,
        double cost,
        List<GeneratorMaterial> materials
) implements IslandUpgradeLevel {
}
