package me.connorjb05.skyblock.model.island.upgrades;

import java.util.EnumMap;
import java.util.Map;

public class IslandUpgradeData {

    private final Map<IslandUpgradeType, Integer> upgradeLevels = new EnumMap<>(IslandUpgradeType.class);

    /**
     * Returns the current level the island has
     * for the specific upgrade type.
     *
     * No stored value means level 0.
     *
     * Level 0 is the default configuration
     * for every island.
     *
     * @param type upgrade type
     * @return level of the island's upgrade
     */
    public int getLevel(IslandUpgradeType type) {
        return upgradeLevels.getOrDefault(type, 0);
    }


    public void setLevel(
            IslandUpgradeType type,
            int level
    ) {

        if (level < 0) {
            throw new IllegalArgumentException(
                    "Upgrade level cannot be negative."
            );
        }

        /*
        Level 0 does not need to occupy
        space in the map.
         */
        if (level == 0) {
            upgradeLevels.remove(type);
            return;
        }

        upgradeLevels.put(type, level);
    }

    public boolean hasUpgrade(IslandUpgradeType type) {
        return getLevel(type) > 0;
    }

    public Map<IslandUpgradeType, Integer> getUpgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}
