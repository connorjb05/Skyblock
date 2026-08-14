package me.connorjb05.skyblock.model.island.upgrades.defintion;

public record MultiplierUpgradeLevel(
        int level,
        double cost,
        double multiplier,
        double chance
) implements IslandUpgradeLevel {

    public MultiplierUpgradeLevel {

        if (level < 0) {
            throw new IllegalArgumentException(
                    "Upgrade level cannot be negative."
            );
        }

        if (cost < 0.0D) {
            throw new IllegalArgumentException(
                    "Upgrade cost cannot be negative."
            );
        }

        if (multiplier < 0.0D) {
            throw new IllegalArgumentException(
                    "Upgrade multiplier cannot be negative."
            );
        }

        if (chance <0.0D || chance > 100.0D) {
            throw new IllegalArgumentException(
                    "Chance must be between 0 and 100."
            );
        }

    }

}
