package me.connorjb05.skyblock.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

@FunctionalInterface
public interface MenuClickEvent {

    void onClick(
            Player player,
            MenuHolder holder,
            String itemId,
            ClickType clickType
    );

}
