package me.connorjb05.skyblock.service.customenchants.condition;

import java.util.EnumMap;
import java.util.Map;

public class ConditionRegistry {

    private final Map<CustomEnchantConditionType, CustomEnchantCondition> conditions =
            new EnumMap<>(
                    CustomEnchantConditionType.class
            );

    public void register(CustomEnchantCondition condition) {
        conditions.put(condition.getType(), condition);
    }

    public CustomEnchantCondition get(CustomEnchantConditionType type) {
        return conditions.get(type);
    }

}
