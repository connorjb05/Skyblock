package me.connorjb05.skyblock.model.island.upgrades;

public record IslandUpgradeEntry(
        long islandId,
        IslandUpgradeType type,
        int level
) {
}
