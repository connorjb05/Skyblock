package me.connorjb05.skyblock.service.customenchants.action.impl;

import me.connorjb05.skyblock.service.customenchants.action.CustomEnchantAction;
import me.connorjb05.skyblock.service.customenchants.action.CustomEnchantActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class CustomEnchantHealAction implements CustomEnchantAction {
    @Override
    public CustomEnchantActionType getType() {
        return CustomEnchantActionType.HEAL;
    }

    @Override
    public void execute(Event e, Player player) {
    }
}
