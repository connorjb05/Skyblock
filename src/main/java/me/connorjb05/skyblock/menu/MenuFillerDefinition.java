package me.connorjb05.skyblock.menu;

import org.bukkit.Material;

public record MenuFillerDefinition(
        boolean enabled,
        Material material,
        String name
) {
}
