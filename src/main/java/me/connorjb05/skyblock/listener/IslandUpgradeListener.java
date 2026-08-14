package me.connorjb05.skyblock.listener;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.upgrades.defintion.GeneratorMaterial;
import me.connorjb05.skyblock.model.island.upgrades.defintion.MultiplierUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.OreGeneratorUpgradeLevel;
import me.connorjb05.skyblock.model.island.upgrades.defintion.SpawnerUpgradeLevel;
import me.connorjb05.skyblock.service.island.IslandUpgradeService;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class IslandUpgradeListener implements Listener {

    private static final Set<Material> CROP_SEEDS = EnumSet.of(
            Material.WHEAT_SEEDS,
            Material.BEETROOT_SEEDS,
            Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS,
            Material.TORCHFLOWER_SEEDS,
            Material.PITCHER_POD
    );

    private final Skyblock plugin;
    private final IslandAccessService accessService;
    private final IslandUpgradeService upgradeService;

    /*
    Groups entities spawned by the same
    spawner during one vanilla spawn cycle.
     */
    private final Map<SpawnerKey, SpawnerBatch> spawnerBatches = new HashMap<>();

    public IslandUpgradeListener(
            Skyblock plugin,
            IslandAccessService accessService,
            IslandUpgradeService upgradeService
    ) {
        this.plugin = plugin;
        this.accessService = accessService;
        this.upgradeService = upgradeService;
    }

    /*
    Ore generator upgrade.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockFormEvent(BlockFormEvent e) {

        Material formedMaterial = e.getNewState().getType();

        if (formedMaterial != Material.COBBLESTONE
                && formedMaterial != Material.STONE) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        Material material = getRandomGeneratorMaterial(
                upgradeService.getOreGeneratorDefinition(island)
        );

        if (material != null) {
            e.getNewState().setType(material);
        }
    }

    private Material getRandomGeneratorMaterial(
            OreGeneratorUpgradeLevel definition
    ) {

        if (definition.materials().isEmpty()) {
            return null;
        }

        double roll = ThreadLocalRandom.current().nextDouble(100.0D);
        double cumulative = 0.0D;
        Material fallback = null;

        for (GeneratorMaterial material : definition.materials()) {

            fallback = material.material();
            cumulative += material.chance();

            if (roll < cumulative) {
                return material.material();
            }
        }

        return fallback;
    }

    /*
    Crop growth upgrade.

    Vanilla already advances the crop once.
    The multiplier adds the remaining stages.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockGrowEvent(BlockGrowEvent e) {

        Material material = e.getBlock().getType();

        if (!isAgeableCrop(material)) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        MultiplierUpgradeLevel definition = upgradeService
                .getCropGrowthDefinition(island);

        double multiplier = definition.multiplier();

        if (multiplier <= 1.0D
                || !rollChance(definition.chance())) {
            return;
        }

        if (!(e.getNewState().getBlockData() instanceof Ageable ageable)) {
            return;
        }

        int bonusStages = calculateGrowthBonus(multiplier);

        if (bonusStages <= 0) {
            return;
        }

        ageable.setAge(
                Math.min(
                        ageable.getMaximumAge(),
                        ageable.getAge() + bonusStages
                )
        );

        e.getNewState().setBlockData(ageable);
    }

    /*
    Crop drop upgrade.

    Minecraft calculates the normal drops first.
    We only change the resulting item amounts.

    Seeds keep their normal vanilla amount.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockDropItemEvent(BlockDropItemEvent e) {

        Material material = e.getBlockState().getType();

        if (!isAgeableCrop(material)) {
            return;
        }

        if (!(e.getBlockState().getBlockData() instanceof Ageable ageable)
                || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlockState().getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        MultiplierUpgradeLevel definition = upgradeService
                .getCropDropsDefinition(island);

        double multiplier = definition.multiplier();

        if (multiplier <= 1.0D
                || !rollChance(definition.chance())) {
            return;
        }

        for (Item item : e.getItems()) {

            if (CROP_SEEDS.contains(item.getItemStack().getType())) {
                continue;
            }

            multiplyItemEntity(
                    item,
                    multiplier
            );
        }
    }

    /*
    Mob drop and experience upgrades.

    This runs before MobStackService at HIGHEST,
    so stacked deaths receive upgraded rewards too.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEntityDeathEvent(EntityDeathEvent e) {

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        Island island = accessService
                .getIslandAt(mob.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        applyMobDrops(
                e,
                island
        );

        applyMobExperience(
                e,
                island
        );
    }

    private void applyMobDrops(
            EntityDeathEvent e,
            Island island
    ) {

        MultiplierUpgradeLevel definition = upgradeService
                .getMobDropsDefinition(island);

        double multiplier = definition.multiplier();

        if (multiplier <= 1.0D
                || e.getDrops().isEmpty()
                || !rollChance(definition.chance())) {
            return;
        }

        multiplyDrops(
                e.getDrops(),
                multiplier
        );
    }

    private void applyMobExperience(
            EntityDeathEvent e,
            Island island
    ) {

        MultiplierUpgradeLevel definition = upgradeService
                .getMobExperienceDefinition(island);

        double multiplier = definition.multiplier();
        int experience = e.getDroppedExp();

        if (multiplier <= 1.0D
                || experience <= 0
                || !rollChance(definition.chance())) {
            return;
        }

        long multiplied = calculateAmount(
                experience,
                multiplier
        );

        e.setDroppedExp(
                (int) Math.min(
                        Integer.MAX_VALUE,
                        multiplied
                )
        );
    }

    /*
    Handles vanilla block spawners.

    Multiple SpawnerSpawnEvents may occur in
    one cycle, so process the batch next tick.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onSpawnerSpawnEvent(SpawnerSpawnEvent e) {

        CreatureSpawner spawner = e.getSpawner();

        if (spawner == null) {
            return;
        }

        Island island = accessService
                .getIslandAt(spawner.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        SpawnerUpgradeLevel definition = upgradeService
                .getSpawnerDefinition(island);

        if (definition.speedMultiplier() <= 1.0D
                && definition.amountMultiplier() <= 1.0D) {
            return;
        }

        SpawnerKey key = new SpawnerKey(
                spawner.getWorld().getUID(),
                spawner.getX(),
                spawner.getY(),
                spawner.getZ()
        );

        SpawnerBatch batch = spawnerBatches.get(key);

        if (batch == null) {

            batch = new SpawnerBatch(
                    spawner.getLocation()
            );

            spawnerBatches.put(
                    key,
                    batch
            );

            plugin.getServer()
                    .getScheduler()
                    .runTask(
                            plugin,
                            () -> processSpawnerBatch(key)
                    );
        }

        batch.entities.add(
                e.getEntity()
        );
    }

    /*
    Multiplies a physical crop drop.

    The first entity is reused and overflow is
    spawned only when the amount exceeds its
    normal ItemStack limit.
     */
    private void multiplyItemEntity(
            Item item,
            double multiplier
    ) {

        ItemStack original = item.getItemStack();

        if (original.isEmpty()) {
            return;
        }

        long amount = calculateAmount(
                original.getAmount(),
                multiplier
        );

        if (amount <= 0) {
            return;
        }

        int maxStackSize = original.getMaxStackSize();
        int firstAmount = (int) Math.min(amount, maxStackSize);

        ItemStack first = original.clone();
        first.setAmount(firstAmount);
        item.setItemStack(first);

        long remaining = amount - firstAmount;

        while (remaining > 0) {

            int nextAmount = (int) Math.min(
                    remaining,
                    maxStackSize
            );

            ItemStack extra = original.clone();
            extra.setAmount(nextAmount);

            item.getWorld().dropItemNaturally(
                    item.getLocation(),
                    extra
            );

            remaining -= nextAmount;
        }
    }

    /*
    Multiplies the complete mob drop list while
    respecting each material's stack limit.
     */
    private void multiplyDrops(
            List<ItemStack> drops,
            double multiplier
    ) {

        List<ItemStack> original = new ArrayList<>(drops);
        drops.clear();

        for (ItemStack stack : original) {

            if (stack.isEmpty()) {
                continue;
            }

            addSplitStacks(
                    drops,
                    stack,
                    calculateAmount(
                            stack.getAmount(),
                            multiplier
                    )
            );
        }
    }

    private void addSplitStacks(
            List<ItemStack> drops,
            ItemStack template,
            long amount
    ) {

        int maxStackSize = template.getMaxStackSize();
        long remaining = amount;

        while (remaining > 0) {

            int nextAmount = (int) Math.min(
                    remaining,
                    maxStackSize
            );

            ItemStack stack = template.clone();
            stack.setAmount(nextAmount);
            drops.add(stack);

            remaining -= nextAmount;
        }
    }

    /*
    Calculates a real multiplier.

    Whole multipliers are exact:
    2 x 100 = 200.

    Fractional results use probabilistic rounding
    so x1.5 averages to a true x1.5 over time.
     */
    private long calculateAmount(
            long amount,
            double multiplier
    ) {

        if (amount <= 0
                || multiplier <= 0.0D
                || !Double.isFinite(multiplier)) {
            return 0L;
        }

        double value = amount * multiplier;

        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long whole = (long) Math.floor(value);
        double remainder = value - whole;

        if (remainder > 0.0D
                && ThreadLocalRandom.current().nextDouble() < remainder) {
            whole++;
        }

        return Math.max(
                0L,
                whole
        );
    }

    /*
    Vanilla already supplies one crop growth stage.
    Only the remaining multiplier is added here.
     */
    private int calculateGrowthBonus(
            double multiplier
    ) {

        double bonus = multiplier - 1.0D;

        if (bonus <= 0.0D) {
            return 0;
        }

        int stages = (int) Math.floor(bonus);
        double remainder = bonus - stages;

        if (remainder > 0.0D
                && ThreadLocalRandom.current().nextDouble() < remainder) {
            stages++;
        }

        return stages;
    }

    private boolean rollChance(
            double chance
    ) {

        if (chance <= 0.0D) {
            return false;
        }

        if (chance >= 100.0D) {
            return true;
        }

        return ThreadLocalRandom.current()
                .nextDouble(100.0D) < chance;
    }

    private void processSpawnerBatch(
            SpawnerKey key
    ) {

        SpawnerBatch batch = spawnerBatches.remove(key);

        if (batch == null) {
            return;
        }

        Block block = batch.location.getBlock();

        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }

        Island island = accessService
                .getIslandAt(block)
                .orElse(null);

        if (island == null) {
            return;
        }

        SpawnerUpgradeLevel definition = upgradeService
                .getSpawnerDefinition(island);

        applySpawnerSpeed(
                spawner,
                definition
        );

        applySpawnerAmount(
                batch,
                definition
        );
    }

    /*
    Spawner speed is always active and does not
    use the amount upgrade chance.
     */
    private void applySpawnerSpeed(
            CreatureSpawner spawner,
            SpawnerUpgradeLevel definition
    ) {

        double multiplier = definition.speedMultiplier();

        if (multiplier <= 1.0D) {
            return;
        }

        int delay = spawner.getDelay();

        if (delay <= 1) {
            return;
        }

        int upgradedDelay = Math.max(
                1,
                (int) Math.round(
                        delay / multiplier
                )
        );

        if (upgradedDelay >= delay) {
            return;
        }

        spawner.setDelay(upgradedDelay);
        spawner.update(true, false);
    }

    /*
    Spawner amount chance is rolled once for the
    complete vanilla spawn cycle.
     */
    private void applySpawnerAmount(
            SpawnerBatch batch,
            SpawnerUpgradeLevel definition
    ) {

        double multiplier = definition.amountMultiplier();

        if (multiplier <= 1.0D
                || !rollChance(definition.chance())) {
            return;
        }

        List<Entity> validEntities = new ArrayList<>(
                batch.entities.size()
        );

        for (Entity entity : batch.entities) {

            if (entity.isValid() && !entity.isDead()) {
                validEntities.add(entity);
            }
        }

        if (validEntities.isEmpty()) {
            return;
        }

        long total = calculateAmount(
                validEntities.size(),
                multiplier
        );

        long extra = total - validEntities.size();

        for (long i = 0; i < extra; i++) {

            Entity source = validEntities.get(
                    (int) (i % validEntities.size())
            );

            Entity copy;

            try {
                copy = source.copy();
            } catch (Throwable throwable) {
                continue;
            }

            copy.spawnAt(
                    source.getLocation(),
                    CreatureSpawnEvent.SpawnReason.CUSTOM
            );
        }
    }

    private boolean isAgeableCrop(
            Material material
    ) {
        return Tag.CROPS.isTagged(material)
                || material == Material.NETHER_WART
                || material == Material.COCOA;
    }

    private record SpawnerKey(
            UUID worldId,
            int x,
            int y,
            int z
    ) {
    }

    private static class SpawnerBatch {

        private final Location location;
        private final List<Entity> entities = new ArrayList<>();

        private SpawnerBatch(
                Location location
        ) {
            this.location = location.clone();
        }
    }
}
