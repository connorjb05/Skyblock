package me.connorjb05.skyblock.service.config.settings;

public record IslandRegionSettings(
        String world,
        int size,
        int spacing,
        int slotsPerRow,
        int originX,
        int originZ
) {
}
