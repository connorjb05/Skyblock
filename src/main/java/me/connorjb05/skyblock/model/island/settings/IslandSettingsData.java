package me.connorjb05.skyblock.model.island.settings;

import java.util.EnumMap;
import java.util.Map;

public class IslandSettingsData {

    private final Map<IslandSetting, Boolean> settings = new EnumMap<>(
            IslandSetting.class
    );

    public boolean getValue(IslandSetting setting) {
        return settings.getOrDefault(
                setting,
                setting.getDefaultValue()
        );
    }

    public boolean hasOverride(IslandSetting setting) {
        return settings.containsKey(setting);
    }

    public void setValue(IslandSetting setting, boolean value) {

        if (value == setting.getDefaultValue()) {
            settings.remove(setting);
            return;
        }

        settings.put(setting, value);
    }

    public void reset(IslandSetting setting) {
        settings.remove(setting);
    }

    public void reset() {
        settings.clear();
    }

}