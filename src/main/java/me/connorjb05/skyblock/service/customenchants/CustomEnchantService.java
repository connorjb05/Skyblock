package me.connorjb05.skyblock.service.customenchants;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.service.customenchants.action.CustomEnchantActionRegistry;
import me.connorjb05.skyblock.service.customenchants.action.impl.CustomEnchantHealAction;

public class CustomEnchantService implements Service {

    private final CustomEnchantActionRegistry actionRegistry = new CustomEnchantActionRegistry();

    @Override
    public void onEnable(Skyblock plugin) {
        loadActionRegistry();
    }

    @Override
    public void onDisable(Skyblock plugin) {

    }

    private void loadActionRegistry() {
        actionRegistry.register(new CustomEnchantHealAction());
    }
}
