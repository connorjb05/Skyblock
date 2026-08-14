package me.connorjb05.skyblock.model.island.upgrades.defintion;

import org.bukkit.Material;

public record GeneratorMaterial(
        Material material,
        double chance
) {
}
