package me.connorjb05.skyblock.service.customenchants;

import me.connorjb05.skyblock.model.CustomEnchant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomEnchantRegistry {

    private final Map<String, CustomEnchant> enchants = new HashMap<>();

    public void register(CustomEnchant customEnchant) {
        enchants.put(customEnchant.getId().toLowerCase(), customEnchant);
    }

    public CustomEnchant get(String id) {
        return enchants.get(id.toLowerCase());
    }

    public Collection<CustomEnchant> getAll() {
        return enchants.values();
    }
}
