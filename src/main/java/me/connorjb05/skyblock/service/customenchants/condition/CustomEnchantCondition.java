package me.connorjb05.skyblock.service.customenchants.condition;

public interface CustomEnchantCondition {

    CustomEnchantConditionType getType();

    boolean conditionMet();
}
