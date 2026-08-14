package me.connorjb05.skyblock.model.island.upgrades.defintion;

public record SpawnerUpgradeLevel(
        int level,
        double cost,
        double speedMultiplier,
        double amountMultiplier,
        double chance
) implements IslandUpgradeLevel {

    public SpawnerUpgradeLevel {

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

        if (speedMultiplier <= 0.0D) {
            throw new IllegalArgumentException(
                    "Spawner speed multiplier must be greater than 0."
            );
        }

        if (amountMultiplier <= 0.0D) {
            throw new IllegalArgumentException(
                    "Spawner amount multiplier must be greater than 0."
            );
        }

        if (chance < 0.0D ||
                chance > 100.0D) {

            throw new IllegalArgumentException(
                    "Chance must be between 0 and 100."
            );
        }
    }

}
