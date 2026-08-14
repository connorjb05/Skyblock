package me.connorjb05.skyblock.service.config.settings;

import org.bukkit.Difficulty;

public record IslandWorldSettings(
        int tickSpeed,
        Difficulty difficulty,
        boolean deathMessages
) {
}
