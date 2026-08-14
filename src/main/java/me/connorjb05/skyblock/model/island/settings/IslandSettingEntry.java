package me.connorjb05.skyblock.model.island.settings;

public record IslandSettingEntry(
        long islandId,
        IslandSetting setting,
        boolean value
) {
}
