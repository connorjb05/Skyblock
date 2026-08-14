package me.connorjb05.skyblock.listener;

import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.settings.IslandSetting;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import me.connorjb05.skyblock.service.island.setting.IslandSettingsService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;

public class IslandSettingsListener implements Listener {

    private final IslandAccessService accessService;
    private final IslandSettingsService settingsService;

    public IslandSettingsListener(
            IslandAccessService accessService,
            IslandSettingsService settingsService
    ) {
        this.accessService = accessService;
        this.settingsService = settingsService;
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {

        if (!(e.getEntity() instanceof Player target)) {
            return;
        }

        Player attacker = getAttackingPlayer(e.getDamager());
        if (attacker == null) {
            return;
        }

        /*
        Do not treat self-damage
        as PvP.
         */
        if (attacker.getUniqueId().equals(target.getUniqueId())) {
            return;
        }

        Island island = accessService
                .getIslandAt(target.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.PVP
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Mob griefing.

    Covers things such as mobs changing,
    consuming or breaking blocks.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent e) {

        /*
        Player block changes are handled
        by IslandProtectionListener.

        This setting only controls
        non-player living entities.
         */
        if (!(e.getEntity() instanceof LivingEntity)
                || e.getEntity() instanceof Player) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.MOB_GRIEFING
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Entity explosions.

    Only remove protected island blocks
    from the explosion's block list.

    The explosion itself may still damage
    entities and produce knockback.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEntityExplodeEvent(EntityExplodeEvent e) {

        e.blockList().removeIf(
                this::isExplosionProtected
        );
    }

    /*
    Block explosions.

    Covers explosions originating from
    blocks rather than entities.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockExplodeEvent(BlockExplodeEvent e) {

        e.blockList().removeIf(
                this::isExplosionProtected
        );
    }

    /*
    Prevent naturally spreading fire
    and lava-created fire.

    Player-created fire is intentionally
    not blocked by this setting.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockIgniteEvent(BlockIgniteEvent e) {

        if (e.getCause()
                != BlockIgniteEvent.IgniteCause.SPREAD
                && e.getCause()
                != BlockIgniteEvent.IgniteCause.LAVA) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.FIRE_SPREAD
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Prevent existing fire from destroying
    blocks when fire spread is disabled.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockBurnEvent(BlockBurnEvent e) {

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.FIRE_SPREAD
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Natural leaf decay.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onLeavesDecayEvent(LeavesDecayEvent e) {

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.LEAF_DECAY
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Player crop/farmland trampling.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPlayerInteractEvent(PlayerInteractEvent e) {

        if (e.getAction() != Action.PHYSICAL) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null
                || block.getType() != Material.FARMLAND) {
            return;
        }

        Island island = accessService
                .getIslandAt(block)
                .orElse(null);

        if (island == null) {
            return;
        }

        if (!settingsService.getValue(
                island,
                IslandSetting.CROP_TRAMPLING
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Mob/entity crop trampling.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEntityInteractEvent(EntityInteractEvent e) {

        if (e.getBlock().getType() != Material.FARMLAND) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getBlock())
                .orElse(null);

        if (island == null) {
            return;
        }

        /*
        Crop trampling must be enabled.

        Mob griefing must also be enabled
        for a living non-player entity to
        damage farmland.
         */
        if (!settingsService.getValue(
                island,
                IslandSetting.CROP_TRAMPLING
        )) {
            e.setCancelled(true);
            return;
        }

        if (e.getEntity() instanceof LivingEntity
                && !(e.getEntity() instanceof Player)
                && !settingsService.getValue(
                island,
                IslandSetting.MOB_GRIEFING
        )) {

            e.setCancelled(true);
        }
    }

    /*
    Controls naturally/system spawned
    animals and monsters.

    Spawners, breeding, spawn eggs,
    commands and plugin-created mobs are
    intentionally allowed.
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onCreatureSpawnEvent(CreatureSpawnEvent e) {

        if (!isControlledSpawnReason(e.getSpawnReason())) {
            return;
        }

        Island island = accessService
                .getIslandAt(e.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        SpawnCategory category = e.getEntity().getSpawnCategory();

        /*
        Hostile mobs.
         */
        if (category == SpawnCategory.MONSTER) {

            if (!settingsService.getValue(
                    island,
                    IslandSetting.MONSTER_SPAWNING
            )) {
                e.setCancelled(true);
            }

            return;
        }

        /*
        Passive / ambient creatures.
         */
        if (isAnimalCategory(category)
                && !settingsService.getValue(
                island,
                IslandSetting.ANIMAL_SPAWNING
        )) {

            e.setCancelled(true);
        }
    }

    /*
    Returns the player responsible for
    an attack when possible.

    Supports direct hits, projectiles,
    TNT and owned tameable mobs.
     */
    private Player getAttackingPlayer(Entity damager) {

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {

            return player;
        }

        if (damager instanceof TNTPrimed tnt
                && tnt.getSource() instanceof Player player) {

            return player;
        }

        if (damager instanceof Tameable tameable
                && tameable.getOwner() instanceof Player player) {

            return player;
        }

        return null;
    }

    /*
    Checks if one explosion block should
    remain untouched.
     */
    private boolean isExplosionProtected(Block block) {

        Island island = accessService
                .getIslandAt(block)
                .orElse(null);

        if (island == null) {
            return false;
        }

        return !settingsService.getValue(
                island,
                IslandSetting.EXPLOSIONS
        );
    }

    /*
    Categories considered animals/passive
    creatures for the island setting.
     */
    private boolean isAnimalCategory(SpawnCategory category) {

        return switch (category) {

            case ANIMAL,
                 WATER_ANIMAL,
                 WATER_AMBIENT,
                 WATER_UNDERGROUND_CREATURE,
                 AMBIENT,
                 AXOLOTL -> true;

            default -> false;
        };
    }

    /*
    World-generated spawning is controlled
    by the island setting.

    Deliberately allow anything players,
    spawners or plugins intentionally create.
     */
    private boolean isControlledSpawnReason(CreatureSpawnEvent.SpawnReason reason) {

        return switch (reason) {

            case SPAWNER,
                 TRIAL_SPAWNER,
                 OMINOUS_ITEM_SPAWNER,

                 SPAWNER_EGG,
                 EGG,
                 DISPENSE_EGG,

                 BREEDING,
                 BUCKET,

                 COMMAND,
                 CUSTOM,

                 BUILD_IRONGOLEM,
                 BUILD_SNOWMAN,
                 BUILD_COPPERGOLEM,
                 BUILD_WITHER,

                 BEEHIVE,
                 SHOULDER_ENTITY -> false;

            default -> true;
        };
    }
}
