package me.connorjb05.skyblock.service.customenchants.action;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface CustomEnchantAction {

    CustomEnchantActionType getType();

    void execute(
            Event e, // Direct event of when trigger occurs
            Player player // player wielding the custom enchant
    );
}
