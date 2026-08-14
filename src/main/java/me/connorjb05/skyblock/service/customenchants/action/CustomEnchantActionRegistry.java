package me.connorjb05.skyblock.service.customenchants.action;

import java.util.EnumMap;
import java.util.Map;

public class CustomEnchantActionRegistry {

    private final Map<CustomEnchantActionType, CustomEnchantAction> actions
            = new EnumMap<>
            (
                    CustomEnchantActionType.class
            );

    public void register(CustomEnchantAction action) {
        actions.put(action.getType(), action);
    }

    public CustomEnchantAction get(CustomEnchantActionType type) {
        return actions.get(type);
    }

}
