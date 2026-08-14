package me.connorjb05.skyblock.service.island;

import me.connorjb05.skyblock.database.repository.IslandUpgradeRepository;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeType;
import me.connorjb05.skyblock.model.island.upgrades.defintion.IslandSizeUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.MultiplierUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.OreGeneratorUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.SpawnerUpgradeLevel;
import me.connorjb05.skyblock.service.config.settings.IslandUpgradeSettings;

import java.util.concurrent.CompletableFuture;

public class IslandUpgradeService {

    private final IslandUpgradeRepository repository;
    private final IslandUpgradeSettings settings;

    public IslandUpgradeService(
            IslandUpgradeRepository repository,
            IslandUpgradeSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public int getIslandSize(Island island) {
        return getIslandSizeDefinition(island).size();
    }

    public double getMobDropsMultiplier(Island island) {
        return getMobDropsDefinition(island)
                .multiplier();
    }

    public double getMobDropChance(Island island) {
        return getMobDropsDefinition(island)
                .chance();
    }

    public double getMobExperienceMultiplier(Island island) {
        return getMobExperienceDefinition(island)
                .multiplier();
    }

    public double getMobExperienceChance(Island island) {
        return getMobExperienceDefinition(island)
                .chance();
    }

    public double getCropGrowthMultiplier(Island island) {
        return getCropGrowthDefinition(island)
                .multiplier();
    }

    public double getCropGrowthChance(Island island) {
        return getCropGrowthDefinition(island)
                .chance();
    }

    public double getCropDropMultiplier(Island island) {
        return getCropDropsDefinition(island)
                .multiplier();
    }

    public double getCropDropChance(Island island) {
        return getCropDropsDefinition(island)
                .chance();
    }

    public double getSpawnerSpeedMultiplier(Island island) {
        return getSpawnerDefinition(island)
                .speedMultiplier();
    }

    public double getSpawnerAmountMultiplier(Island island) {
        return getSpawnerDefinition(island)
                .amountMultiplier();
    }

    public double getSpawnerChance(Island island) {
        return getSpawnerDefinition(island)
                .chance();
    }

    public int getOreGeneratorTier(Island island) {
        return getOreGeneratorDefinition(island)
                .level();
    }

    public IslandSizeUpgradeLevel getIslandSizeDefinition(Island island) {

        int level = getLevel(island, IslandUpgradeType.ISLAND_SIZE);

        IslandSizeUpgradeLevel definition = settings.islandSize()
                .get(level);

        if (definition == null) {
            throw new IllegalStateException(
                    "Missing island size upgrade definition for level " + level
            );
        }

        return definition;
    }

    public MultiplierUpgradeLevel getMobDropsDefinition(
            Island island
    ) {
        return settings.mobDrops()
                .get(getLevel(
                        island,
                        IslandUpgradeType.MOB_DROPS
                ));
    }

    public MultiplierUpgradeLevel getMobExperienceDefinition(
            Island island
    ) {
        return settings.mobExperience()
                .get(getLevel(
                        island,
                        IslandUpgradeType.MOB_EXPERIENCE
                ));
    }

    public MultiplierUpgradeLevel getCropGrowthDefinition(
            Island island
    ) {
        return settings.cropGrowth()
                .get(getLevel(
                        island,
                        IslandUpgradeType.CROP_GROWTH
                ));
    }

    public MultiplierUpgradeLevel getCropDropsDefinition(
            Island island
    ) {
        return settings.cropDrops()
                .get(getLevel(
                        island,
                        IslandUpgradeType.CROP_DROPS
                ));
    }

    public SpawnerUpgradeLevel getSpawnerDefinition(
            Island island
    ) {
        return settings.spawner()
                .get(getLevel(
                        island,
                        IslandUpgradeType.SPAWNER
                ));
    }

    public OreGeneratorUpgradeLevel getOreGeneratorDefinition(
            Island island
    ) {
        return settings.oreGenerator()
                .get(getLevel(
                        island,
                        IslandUpgradeType.ORE_GENERATOR
                ));
    }

    public int getLevel(
            Island island,
            IslandUpgradeType type
    ) {
        if (island == null) {
            return 0;
        }
        return island.getUpgradeData().getLevel(type);
    }

    /*
    Updates the database first.

    Only after the database operation
    succeeds we update the cached island object
     */
    public CompletableFuture<Void> setLevel(
            Island island,
            IslandUpgradeType type,
            int level
    ) {

        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Island cannot be null."
            ));
        }

        if (level < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Upgrade level cannot be negative."
            ));
        }

        return repository
                .setLevel(
                        island.getIslandId(),
                        type,
                        level
                )
                .thenRun(() -> {
                    island.getUpgradeData()
                            .setLevel(
                                    type,
                                    level
                            );
                });
    }


    public CompletableFuture<Integer> increaseLevel(
            Island island,
            IslandUpgradeType type
    ) {

        int newLevel = island.getUpgradeData().getLevel(type) + 1;

        return setLevel(
                island,
                type,
                newLevel
        ).thenApply(ignored -> newLevel);
    }

    public CompletableFuture<Void> reset(
            Island island,
            IslandUpgradeType type
    ) {
        return setLevel(
                island,
                type,
                0
        );
    }
}
