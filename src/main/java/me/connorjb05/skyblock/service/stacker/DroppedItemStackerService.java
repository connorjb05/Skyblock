package me.connorjb05.skyblock.service.stacker;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.service.config.settings.ItemStackerSettings;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DroppedItemStackerService implements Service, Listener {

    private static final long MAX_STACK_SIZE = 1_000_000_000L;

    private final IslandAccessService accessService;

    private Skyblock plugin;

    private NamespacedKey stackSizeKey;

    private BukkitTask bootstrapTask;

    private boolean enabled;

    private double stackRadius;
    private double stackYRadius;
    private double stackRadiusSquared;

    private String stackName;

    private final Set<UUID> pendingStacks = new HashSet<>();

    public DroppedItemStackerService(IslandAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public void onEnable(Skyblock plugin) {

        this.plugin = plugin;

        ItemStackerSettings settings = plugin
                .getConfigService()
                .getItemStackerSettings();

        enabled = settings.enabled();

        if (!enabled) {
            return;
        }

        stackRadius = settings.radius();
        stackYRadius = settings.radiusY();
        stackRadiusSquared = stackRadius * stackRadius;

        stackName = settings.name();

        if (stackRadius <= 0.0D) {
            throw new IllegalStateException(
                    "Item stacker radius must be greater than 0."
            );
        }

        if (stackYRadius < 0.0D) {
            throw new IllegalStateException(
                    "Item stacker Y radius cannot be negative."
            );
        }

        if (stackName == null || stackName.isBlank()) {
            stackName =
                    "<gold><bold>x<amount></bold> <white><item>";
        }

        stackSizeKey = new NamespacedKey(
                plugin,
                "item_stack_size"
        );

        Bukkit.getPluginManager()
                .registerEvents(this, plugin);

        bootstrapTask = Bukkit.getScheduler()
                .runTask(
                        plugin,
                        this::stackLoadedItems
                );
    }

    @Override
    public void onDisable(Skyblock plugin) {

        HandlerList.unregisterAll(this);

        if (bootstrapTask != null) {
            bootstrapTask.cancel();
            bootstrapTask = null;
        }

        pendingStacks.clear();

        enabled = false;
        stackSizeKey = null;
        this.plugin = null;
    }

    /*
    Handles newly spawned items and
    items loaded from chunks.
     */
    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onEntityAddToWorldEvent(EntityAddToWorldEvent e) {

        if (!(e.getEntity() instanceof Item item)) {
            return;
        }

        if (!accessService.isIslandWorld(e.getWorld())) {
            return;
        }

        /*
        Queue instead of blindly scheduling
        another stacking operation.

        Multiple events involving this item
        will collapse into one pending task.
         */
        queueStack(item);
    }

    /*
    Prevent vanilla item merging inside
    island regions.

    Vanilla does not understand our
    logical PDC stack amount.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onItemMergeEvent(ItemMergeEvent e) {

        Item source = e.getEntity();
        Item target = e.getTarget();

        Island sourceIsland = accessService
                .getIslandAt(source.getLocation())
                .orElse(null);

        Island targetIsland = accessService
                .getIslandAt(target.getLocation())
                .orElse(null);

        /*
        Neither item is inside an island.
        Let vanilla handle the merge.
         */
        if (sourceIsland == null && targetIsland == null) {
            return;
        }

        /*
        Vanilla cannot understand our logical
        stack amount, so never let vanilla merge
        items inside an island.
         */
        e.setCancelled(true);

        if (sourceIsland == null || targetIsland == null) {
            return;
        }

        if (sourceIsland.getIslandId() != targetIsland.getIslandId()) {
            return;
        }

        /*
        Do NOT directly schedule merge(source, target).

        Both entities instead enter the same
        deduplicated stacking queue.
         */
        queueStack(source);
        queueStack(target);
    }

    /*
    Handles player pickup of logical stacks
    larger than vanilla's ItemStack limit.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityPickupItemEvent(EntityPickupItemEvent e) {

        Item item = e.getItem();

        long logicalAmount = getStackSize(item);

        int physicalAmount =
                item.getItemStack()
                        .getAmount();

        /*
        Vanilla can safely handle
        a normal physical stack.
         */
        if (logicalAmount <= physicalAmount) {
            return;
        }

        /*
        Do not allow mobs to consume
        logical item stacks.
         */
        if (!(e.getEntity() instanceof Player player)) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        long inserted = addToInventory(
                player.getInventory(),
                item.getItemStack(),
                logicalAmount
        );

        if (inserted <= 0) {
            return;
        }

        long remaining =
                logicalAmount - inserted;

        if (remaining <= 0) {
            item.remove();
        } else {
            setStackSize(
                    item,
                    remaining
            );
        }

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ITEM_PICKUP,
                0.2F,
                1.0F
        );
    }

    /*
    Handles hopper and hopper-minecart
    pickup of logical item stacks.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInventoryPickupItemEvent(InventoryPickupItemEvent e) {

        Item item = e.getItem();

        long logicalAmount = getStackSize(item);

        int physicalAmount =
                item.getItemStack()
                        .getAmount();

        /*
        Vanilla can handle ordinary
        physical item stacks.
         */
        if (logicalAmount <= physicalAmount) {
            return;
        }

        e.setCancelled(true);

        ItemStack template =
                item.getItemStack();

        /*
        One hopper attempt may consume at most
        one normal ItemStack worth of items.
         */
        long transferLimit = Math.min(
                logicalAmount,
                template.getMaxStackSize()
        );

        long inserted = addToInventory(
                e.getInventory(),
                template,
                transferLimit
        );

        if (inserted <= 0) {
            return;
        }

        long remaining =
                logicalAmount - inserted;

        if (remaining <= 0) {
            item.remove();
        } else {
            setStackSize(
                    item,
                    remaining
            );
        }
    }

    private void queueStack(Item item) {

        UUID uuid = item.getUniqueId();

        /*
        Only allow one pending stacking
        operation per item entity.
         */
        if (!pendingStacks.add(uuid)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {

            try {

                if (!enabled || !item.isValid() || item.isDead()) {
                    return;
                }

                tryStack(item);

            } finally {
                pendingStacks.remove(uuid);
            }
        });
    }

    /*
    Attempts to merge an item into nearby
    compatible stacks from the same island.

    No database calls.
    No world-wide scans.
    No repeating scheduler.
     */
    public void tryStack(Item incoming) {

        if (!enabled) {
            return;
        }

        if (!canStack(incoming)) {
            return;
        }

        Island island = accessService
                .getIslandAt(incoming.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        Collection<Item> nearby = incoming.getLocation()
                .getNearbyEntitiesByType(
                        Item.class,
                        stackRadius,
                        stackYRadius,
                        item -> item != incoming
                                && item.isValid()
                                && !item.isDead()
                );

        if (nearby.isEmpty()) {
            setStackSize(
                    incoming,
                    getStackSize(incoming)
            );
            return;
        }

        /*
        Larger stacks first.

        This minimizes the amount
        of physical item entities.
         */
        ArrayList<Item> candidates = new ArrayList<>(nearby);

        candidates.sort(
                Comparator
                        .comparingLong(this::getStackSize)
                        .reversed()
        );

        double incomingX = incoming.getX();
        double incomingZ = incoming.getZ();

        for (Item target : candidates) {

            if (!incoming.isValid()) {
                return;
            }

            if (!canStack(target)) {
                continue;
            }

            /*
            Nearby lookup uses a bounding box.

            Do a cheap squared-distance check
            for an actual circular radius.
             */
            double differenceX = incomingX - target.getX();
            double differenceZ = incomingZ - target.getZ();

            double distanceSquared =
                    differenceX * differenceX
                            + differenceZ * differenceZ;

            if (distanceSquared > stackRadiusSquared) {
                continue;
            }

            Island targetIsland = accessService
                    .getIslandAt(target.getLocation())
                    .orElse(null);

            if (targetIsland == null ||
                    targetIsland.getIslandId() != island.getIslandId()) {
                continue;
            }

            if (!isCompatible(incoming, target)) {
                continue;
            }

            merge(
                    incoming,
                    target
            );
        }

        if (incoming.isValid()) {
            setStackSize(
                    incoming,
                    getStackSize(incoming)
            );
        }
    }

    /*
    Merges source into target.

    Returns true if anything
    was successfully transferred.
     */
    private boolean merge(Item source, Item target) {

        if (source == target ||
                !source.isValid() ||
                !target.isValid()) {
            return false;
        }

        if (!isCompatible(source, target)) {
            return false;
        }

        Island sourceIsland = accessService
                .getIslandAt(source.getLocation())
                .orElse(null);

        Island targetIsland = accessService
                .getIslandAt(target.getLocation())
                .orElse(null);

        if (sourceIsland == null ||
                targetIsland == null ||
                sourceIsland.getIslandId() != targetIsland.getIslandId()) {
            return false;
        }

        long sourceAmount = getStackSize(source);
        long targetAmount = getStackSize(target);

        if (targetAmount >= MAX_STACK_SIZE) {
            return false;
        }

        long available = MAX_STACK_SIZE - targetAmount;
        long transfer = Math.min(sourceAmount, available);

        if (transfer <= 0) {
            return false;
        }

        /*
        Preserve the strictest pickup delay
        and the oldest valid item age.

        Freshly spawned item entities can have
        ticksLived == 0. Paper does not allow
        explicitly setting ticksLived to 0, so
        only apply the merged age when it is > 0.
         */
        int mergedPickupDelay = Math.max(
                target.getPickupDelay(),
                source.getPickupDelay()
        );

        int mergedTicksLived = Math.max(
                target.getTicksLived(),
                source.getTicksLived()
        );

        long remaining = sourceAmount - transfer;

        /*
        Consume/update the source before
        crediting the target.
         */
        if (remaining <= 0) {
            source.remove();
        } else {
            setStackSize(source, remaining);
        }

        setStackSize(
                target,
                targetAmount + transfer
        );

        target.setPickupDelay(
                mergedPickupDelay
        );

        if (mergedTicksLived > 0) {
            target.setTicksLived(
                    mergedTicksLived
            );
        }

        return true;
    }

    /*
    Returns the logical amount represented
    by this physical Item entity.
     */
    public long getStackSize(Item item) {

        if (stackSizeKey == null) {
            return item.getItemStack().getAmount();
        }

        Long amount = item.getPersistentDataContainer()
                .get(
                        stackSizeKey,
                        PersistentDataType.LONG
                );

        if (amount == null || amount <= 0) {
            return item.getItemStack().getAmount();
        }

        return amount;
    }

    public boolean isStacked(Item item) {
        return getStackSize(item) > 1;
    }

    public boolean isOverflowStack(Item item) {
        return getStackSize(item) > item.getItemStack().getAmount();
    }

    /*
    Sets the logical amount represented
    by this physical Item entity.
     */
    public void setStackSize(
            Item item,
            long amount
    ) {

        if (stackSizeKey == null) {
            throw new IllegalStateException(
                    "DroppedItemStackerService has not been enabled."
            );
        }

        if (amount < 1 || amount > MAX_STACK_SIZE) {
            throw new IllegalArgumentException(
                    "Invalid item stack size: "
                            + amount
            );
        }

        ItemStack stack = item.getItemStack()
                .clone();

        /*
        Keep the physical stack within
        Minecraft's normal item limit.

        PDC contains the true logical amount.
         */
        int physicalAmount = (int) Math.min(
                amount,
                stack.getMaxStackSize()
        );

        stack.setAmount(
                physicalAmount
        );

        item.setItemStack(
                stack
        );

        /*
        Keep stack data even for x1.

        That allows our own visible x1
        hologram without canStack mistaking
        it for an external custom name.
         */
        item.getPersistentDataContainer()
                .set(
                        stackSizeKey,
                        PersistentDataType.LONG,
                        amount
                );

        refreshDisplay(item);
    }

    /*
    Adds logical items to an inventory
    while respecting vanilla stack limits.

    Returns the amount successfully inserted.
     */
    private long addToInventory(
            Inventory inventory,
            ItemStack template,
            long amount
    ) {

        if (amount <= 0) {
            return 0L;
        }

        int maxPerStack = Math.min(
                template.getMaxStackSize(),
                inventory.getMaxStackSize()
        );

        if (maxPerStack <= 0) {
            return 0L;
        }

        long remaining = amount;
        long inserted = 0L;

        while (remaining > 0) {

            int requested = (int) Math.min(
                    remaining,
                    maxPerStack
            );

            ItemStack toAdd =
                    template.asQuantity(requested);

            Map<Integer, ItemStack> leftovers =
                    inventory.addItem(toAdd);

            int leftoverAmount = 0;

            for (ItemStack leftover : leftovers.values()) {
                leftoverAmount += leftover.getAmount();
            }

            int stored =
                    requested - leftoverAmount;

            if (stored <= 0) {
                break;
            }

            inserted += stored;
            remaining -= stored;

            /*
            Part of this requested stack
            did not fit.

            The inventory is full.
             */
            if (leftoverAmount > 0) {
                break;
            }
        }

        return inserted;
    }

    /*
    Determines whether this Item entity
    can participate in stacking.
     */
    private boolean canStack(Item item) {

        if (!item.isValid() || item.isDead()) {
            return false;
        }

        ItemStack stack =
                item.getItemStack();

        if (stack.isEmpty()) {
            return false;
        }

        /*
        Don't overwrite names created
        by another plugin.

        Our own stack names have
        our PDC key attached.
         */
        if (!hasStackData(item) && item.customName() != null) {
            return false;
        }

        return true;
    }

    private boolean isCompatible(
            Item first,
            Item second
    ) {

        /*
        isSimilar compares the complete
        ItemStack while ignoring amount.
         */
        if (!first.getItemStack()
                .isSimilar(
                        second.getItemStack()
                )) {

            return false;
        }

        /*
        Preserve pickup ownership.

        Items with different owners should
        not be merged because that could
        change who is allowed to pick them up.
         */
        if (!Objects.equals(
                first.getOwner(),
                second.getOwner()
        )) {

            return false;
        }

        /*
        Thrower is intentionally ignored.

        Block drops normally have no thrower
        while manually dropped player items do.

        Requiring matching throwers prevents
        otherwise identical items from stacking.
         */

        if (first.canMobPickup() != second.canMobPickup()) {
            return false;
        }

        if (first.canPlayerPickup() != second.canPlayerPickup()) {
            return false;
        }

        if (first.willAge() != second.willAge()) {
            return false;
        }

        return first.isUnlimitedLifetime()
                == second.isUnlimitedLifetime();
    }

    private boolean hasStackData(Item item) {

        return stackSizeKey != null
                && item.getPersistentDataContainer()
                .has(
                        stackSizeKey,
                        PersistentDataType.LONG
                );
    }

    private void refreshDisplay(Item item) {

        long amount = getStackSize(item);

        ItemStack stack = item.getItemStack();

        Component name = plugin
                .getMessageService()
                .parse(
                        stackName,

                        Placeholder.unparsed(
                                "amount",
                                Long.toString(amount)
                        ),

                        Placeholder.component(
                                "item",
                                stack.effectiveName()
                        )
                );

        item.customName(name);
        item.setCustomNameVisible(true);
    }

    /*
    One-time startup pass.

    Future items are handled entirely
    through EntityAddToWorldEvent.
     */
    private void stackLoadedItems() {

        bootstrapTask = null;

        if (!enabled) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {

            if (!accessService.isIslandWorld(world)) {
                continue;
            }

            ArrayList<Item> items =
                    new ArrayList<>(world.getEntitiesByClass(Item.class));

            for (Item item : items) {

                if (!item.isValid()) {
                    continue;
                }

                queueStack(item);
            }
        }
    }
}
