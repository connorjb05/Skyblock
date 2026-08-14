package me.connorjb05.skyblock.model.island.settings;

public enum IslandSetting {

    PVP(false),
    PUBLIC(true),
    MOB_GRIEFING(false),
    EXPLOSIONS(false),
    FIRE_SPREAD(false),
    LEAF_DECAY(false),
    CROP_TRAMPLING(false),

    ANIMAL_SPAWNING(true),
    MONSTER_SPAWNING(true);

    private final boolean defaultValue;

    IslandSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }


}
