package me.connorjb05.skyblock.model.island.upgrades.defintion;

public record IslandSizeUpgradeLevel(
        int level,
        double cost,
        int size
) implements IslandUpgradeLevel {
}
