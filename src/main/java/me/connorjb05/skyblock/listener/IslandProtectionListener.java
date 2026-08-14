package me.connorjb05.skyblock.listener;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;

public class IslandProtectionListener implements Listener {

    private final IslandAccessService accessService;

    public IslandProtectionListener(IslandAccessService accessService) {
        this.accessService = accessService;
    }

    /*
    ========================================
    BUILD PERMISSION EVENTS
    ========================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreakEvent(BlockBreakEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockPlaceEvent(BlockPlaceEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketEmptyEvent(PlayerBucketEmptyEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketFillEvent(PlayerBucketFillEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }

    }

    /*
    Protect stuff like armor stands,
    boats, minecarts and end crystals that
    are placed as entities instead of blocks.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityPlaceEvent(EntityPlaceEvent e) {

        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getEntity().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Paintings and item frames are
    hanging entities.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onHangingPlaceEvent(HangingPlaceEvent e) {

        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getEntity().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onHangingBreakEvent(HangingBreakByEntityEvent e) {

        Player player = getResponsiblePlayer(e.getRemover());
        if (player == null) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getEntity().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Prevent visitors from using
    bonemeal to alter island blocks.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockFertilizeEvent(BlockFertilizeEvent e) {

        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        for (BlockState state : e.getBlocks()) {

            if (accessService.checkPermission(
                    player,
                    state.getLocation(),
                    IslandPermission.BUILD
            )) {
                continue;
            }

            e.setCancelled(true);
            return;
        }

    }

    /*
    Harvesting can modify a block without
    actually breaking it.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onHarvestBlockEvent(PlayerHarvestBlockEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getHarvestedBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }

    }

    /*
    Prevent shearing blocks such as
    beehives/pumpkins where applicable.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onShearBlockEvent(PlayerShearBlockEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }

    }

    /*
    Flint and steel/fire charges can
    modify an island without placing
    a normal block.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockIgniteEvent(BlockIgniteEvent e) {

        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }

    }

    /*
    Sign text is part of the island's
    editable block state.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onSignChangeEvent(SignChangeEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getBlock().getLocation(),
                IslandPermission.BUILD
        )) {
            e.setCancelled(true);
        }

    }

    /*
    ========================================
    DROP / PICK UP ITEMS PERMISSION EVENTS
    ========================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDropItemEvent(PlayerDropItemEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getItemDrop().getLocation(),
                IslandPermission.DROP_AND_PICKUP_ITEMS
        )) {
            e.setCancelled(true);
        }

    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPickupItemEvent(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getItem().getLocation(),
                IslandPermission.DROP_AND_PICKUP_ITEMS
        )) {
            e.setCancelled(true);
        }

    }

    /*
    ========================================
    BLOCK INTERACTIONS

    Containers
    Doors
    Redstone
    ========================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInteractEvent(PlayerInteractEvent e) {

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = e.getPlayer();

        if (e.getAction() == Action.PHYSICAL) {

            if (isRedstoneControl(block)) {

                if (!accessService.checkPermission(
                        player,
                        block.getLocation(),
                        IslandPermission.USE_REDSTONE
                )) {
                    e.setCancelled(true);
                }

                return;
            }


            // Physical block modification.
            if (block.getType() == Material.FARMLAND
                    || block.getType() == Material.TURTLE_EGG) {

                if (!accessService.checkPermission(
                        player,
                        block.getLocation(),
                        IslandPermission.BUILD
                )) {
                    e.setCancelled(true);
                }
            }

            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (isContainer(block)) {

            if (!accessService.checkPermission(
                    player,
                    block.getLocation(),
                    IslandPermission.USE_CONTAINERS
            )) {
                e.setCancelled(true);
            }

            return;
        }

        if (isDoor(block)) {

            if (!accessService.checkPermission(
                    player,
                    block.getLocation(),
                    IslandPermission.USE_DOORS
            )) {
                e.setCancelled(true);
            }

            return;
        }

        if (isRedstoneControl(block)) {

            if (!accessService.checkPermission(
                    player,
                    block.getLocation(),
                    IslandPermission.USE_REDSTONE
            )) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInventoryOpenEvent(InventoryOpenEvent e) {

        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof Player) {
            return;
        }

        Location location = e.getInventory().getLocation();
        if (location == null) {
            return;
        }

        /*
        Horse/chest boat/minecart/etc.
        inventories are entity interactions
         */
        if (holder instanceof Entity) {

            if (!accessService.checkPermission(
                    player,
                    location,
                    IslandPermission.INTERACT_ENTITIES
            )) {
                e.setCancelled(true);
            }

            return;
        }

        if (!accessService.checkPermission(
                player,
                location,
                IslandPermission.USE_CONTAINERS
        )) {
            e.setCancelled(true);
        }
    }

    /*
    ========================================
    ENTITY INTERACTION
    ========================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInteractEntityEvent(
            PlayerInteractEntityEvent e
    ) {

        Entity entity = e.getRightClicked();

        /*
        TODO PVP permission/setting
         */
        if (entity instanceof Player) {
            return;
        }

        if (!accessService.checkPermission(
                e.getPlayer(),
                entity.getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Armor stands specifically require
    PlayerInteractAtEntityEvent handling.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInteractAtEntityEvent(PlayerInteractAtEntityEvent e) {

        if (!(e.getRightClicked() instanceof ArmorStand)) {
            return;
        }

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getRightClicked().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {
            e.setCancelled(true);
        }
    }

    /*
    Prevent damaging island entities.

    Projectiles fired by players are
    included as well.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDamageEvent(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {
            return;
        }

        Player player = getResponsiblePlayer(e.getDamager());
        if (player == null) {
            return;
        }

        if (!accessService.checkPermission(
                player,
                e.getEntity().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onShearEntityEvent(
            PlayerShearEntityEvent e
    ) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getEntity().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onLeashEntityEvent(PlayerLeashEntityEvent e) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getEntity().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {
            e.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onUnleashEntityEvent(
            PlayerUnleashEntityEvent e
    ) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getEntity().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {

            e.setCancelled(true);
        }
    }

    /*
    Prevent players from capturing
    island entities in buckets.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketEntityEvent(
            PlayerBucketEntityEvent e
    ) {

        if (!accessService.checkPermission(
                e.getPlayer(),
                e.getEntity().getLocation(),
                IslandPermission.INTERACT_ENTITIES
        )) {

            e.setCancelled(true);
        }
    }

    /*
    ========================================
    HELPERS
    ========================================
     */

    private boolean isContainer(Block block) {

        if (block.getState() instanceof InventoryHolder) {
            return true;
        }

        return block.getType() == Material.ENDER_CHEST;
    }

    private boolean isDoor(
            Block block
    ) {

        Material material = block.getType();

        return Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material);
    }

    private boolean isRedstoneControl(Block block) {

        Material material = block.getType();

        return Tag.BUTTONS.isTagged(material)
                || Tag.PRESSURE_PLATES.isTagged(material)
                || material == Material.LEVER
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || material == Material.DAYLIGHT_DETECTOR
                || material == Material.TRIPWIRE
                || material == Material.TRIPWIRE_HOOK
                || material == Material.NOTE_BLOCK;
    }

    /*
    Finds the player responsible for
    a direct attack or projectile.
     */
    private Player getResponsiblePlayer(Entity entity) {

        if (entity instanceof Player player) {
            return player;
        }

        if (entity instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }

}
