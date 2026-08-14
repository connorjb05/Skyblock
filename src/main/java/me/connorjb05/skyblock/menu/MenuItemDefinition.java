package me.connorjb05.skyblock.menu;

import org.bukkit.Material;

import java.util.List;

public record MenuItemDefinition(
        boolean enabled,
        List<Integer> slots,
        Material material,
        int amount,
        String name,
        List<String> lore,
        boolean glow
) {
}
