package me.connorjb05.skyblock.service.stacker;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.service.config.settings.MobStackerSettings;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MobStackService implements Service, Listener {

    private static final long MAX_STACK_SIZE = 1_000_000L;

    /*
    Process the stacking queue four times
    per second.

    This gives quick stacking without
    constantly scanning every mob.
     */
    private static final long STACK_PROCESS_PERIOD = 5L;

    /*
    Hard limit on nearby stack checks
    during one processing pass.

    This protects the server if a huge
    amount of mobs spawn/move at once.
     */
    private static final int MAX_STACK_ATTEMPTS_PER_PASS = 128;

    private final IslandAccessService accessService;

    private Skyblock plugin;

    private NamespacedKey stackSizeKey;

    private BukkitTask bootstrapTask;
    private BukkitTask stackProcessTask;

    private boolean enabled;

    private double stackRadius;
    private double stackYRadius;
    private double stackRadiusSquared;

    private String stackName;

    /*
    Mobs waiting for another stacking attempt.

    UUID lookup prevents the same mob from
    being queued hundreds of times while moving.
     */
    private final Queue<Mob> stackQueue = new ArrayDeque<>();
    private final Set<UUID> queuedMobs = new HashSet<>();

    public MobStackService(IslandAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public void onEnable(Skyblock plugin) {

        this.plugin = plugin;

        MobStackerSettings settings = plugin
                .getConfigService()
                .getMobStackerSettings();

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
                    "Mob stacker radius must be greater than 0."
            );
        }

        if (stackYRadius < 0.0D) {
            throw new IllegalStateException(
                    "Mob stacker Y radius cannot be negative."
            );
        }

        if (stackName == null || stackName.isBlank()) {
            stackName =
                    "<gold><bold>x<amount></bold> <white><mob>";
        }

        stackSizeKey = new NamespacedKey(
                plugin,
                "mob_stack_size"
        );

        Bukkit.getPluginManager()
                .registerEvents(this, plugin);

        /*
        Queue loaded mobs once after startup.

        Stacking itself is spread across
        multiple ticks by the processing queue.
         */
        bootstrapTask = Bukkit.getScheduler()
                .runTask(
                        plugin,
                        this::queueLoadedMobs
                );

        /*
        Process only mobs that actually need
        another stacking attempt.
         */
        stackProcessTask = Bukkit.getScheduler()
                .runTaskTimer(
                        plugin,
                        this::processStackQueue,
                        1L,
                        STACK_PROCESS_PERIOD
                );
    }

    @Override
    public void onDisable(Skyblock plugin) {

        HandlerList.unregisterAll(this);

        if (bootstrapTask != null) {
            bootstrapTask.cancel();
            bootstrapTask = null;
        }

        if (stackProcessTask != null) {
            stackProcessTask.cancel();
            stackProcessTask = null;
        }

        stackQueue.clear();
        queuedMobs.clear();

        enabled = false;
        stackSizeKey = null;

        this.plugin = null;
    }

    /*
    Newly spawned or newly loaded mobs
    are queued for stacking.
     */
    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onEntityAddToWorldEvent(EntityAddToWorldEvent e) {

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!accessService.isIslandWorld(e.getWorld())) {
            return;
        }

        queueStack(mob);
    }

    /*
    Re-attempt stacking when mobs actually
    move into another block.

    EntityMoveEvent fires much more often
    than this check is needed, so sub-block
    movement is ignored.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onEntityMoveEvent(EntityMoveEvent e) {

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!accessService.isIslandWorld(mob.getWorld())) {
            return;
        }

        Location from = e.getFrom();
        Location to = e.getTo();

        /*
        Ignore tiny movement inside
        the same Minecraft block.
         */
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        queueStack(mob);
    }

    /*
    When a stacker-managed mob falls into the void,
    remove the entire logical stack at once.

    No drops or experience are produced and the
    stack is not decremented one mob at a time.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityVoidDamageEvent(EntityDamageEvent e) {

        if (e.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!accessService.isIslandWorld(mob.getWorld())) {
            return;
        }

        if (!hasStackData(mob)) {
            return;
        }

        e.setCancelled(true);

        queuedMobs.remove(mob.getUniqueId());
        mob.remove();
    }

    /*
    When one logical mob in a stack dies,
    keep the physical entity alive and
    decrease the logical stack by one.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDeathEvent(EntityDeathEvent e) {

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!accessService.isIslandWorld(mob.getWorld())) {
            return;
        }

        long stackSize = getStackSize(mob);

        /*
        Normal single mob.

        Let Minecraft process the
        death normally.
         */
        if (stackSize <= 1) {
            return;
        }

        List<ItemStack> drops = e.getDrops()
                .stream()
                .map(ItemStack::clone)
                .toList();

        int experience = e.getDroppedExp();

        Location location = mob.getLocation().clone();
        World world = mob.getWorld();

        /*
        Keep the physical entity alive.

        Paper already provides the valid revive state
        when a death event is cancelled, so no manual
        health override is needed here.
         */
        e.setCancelled(true);

        e.getDrops().clear();
        e.setDroppedExp(0);

        setStackSize(
                mob,
                stackSize - 1
        );

        for (ItemStack drop : drops) {

            if (drop.isEmpty()) {
                continue;
            }

            world.dropItemNaturally(
                    location,
                    drop
            );
        }

        if (experience > 0) {
            world.spawn(
                    location,
                    ExperienceOrb.class,
                    orb -> orb.setExperience(experience)
            );
        }

        /*
        The surviving physical mob may now
        be near another compatible stack.
         */
        queueStack(mob);
    }

    /*
    Removes the stacker's custom x1 name immediately
    before a real final mob death completes.

    Stacked deaths are cancelled above and therefore
    never reach this MONITOR handler.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onFinalEntityDeathEvent(EntityDeathEvent e) {

        if (!(e.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!accessService.isIslandWorld(mob.getWorld())) {
            return;
        }

        if (!hasStackData(mob) || getStackSize(mob) > 1) {
            return;
        }

        clearStackData(mob);
    }

    /*
    Adds a mob to the stack queue.

    A mob UUID may only exist once
    in the queue at a time.
     */
    private void queueStack(Mob mob) {

        if (!enabled) {
            return;
        }

        UUID uuid = mob.getUniqueId();

        if (!queuedMobs.add(uuid)) {
            return;
        }

        stackQueue.offer(mob);
    }

    /*
    Processes a bounded amount of work.

    This is the performance safeguard that
    prevents a sudden mob flood from causing
    one giant stacking spike.
     */
    private void processStackQueue() {

        if (!enabled) {
            return;
        }

        int processed = 0;

        while (processed < MAX_STACK_ATTEMPTS_PER_PASS) {

            Mob mob = stackQueue.poll();

            if (mob == null) {
                break;
            }

            queuedMobs.remove(
                    mob.getUniqueId()
            );

            processed++;

            if (!mob.isValid() || mob.isDead()) {
                continue;
            }

            tryStack(mob);
        }
    }

    /*
    Attempts to merge this mob into every
    compatible nearby stack from the same island.

    No database calls.
    No world-wide scans.
     */
    public void tryStack(Mob incoming) {

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

        long remaining = getStackSize(incoming);

        if (remaining <= 0) {
            return;
        }

        Collection<Mob> nearby = incoming.getLocation()
                .getNearbyEntitiesByType(
                        Mob.class,
                        stackRadius,
                        stackYRadius,
                        mob -> mob != incoming
                                && mob.getType() == incoming.getType()
                );

        /*
        Nothing to merge with.

        Still establish our stack data and
        show the x1 hologram.
         */
        if (nearby.isEmpty()) {
            setStackSize(
                    incoming,
                    remaining
            );

            return;
        }

        /*
        Larger stacks first.

        This packs mobs into the fewest
        physical entities possible.
         */
        ArrayList<Mob> candidates = new ArrayList<>(nearby);

        candidates.sort(
                Comparator
                        .comparingLong(this::getStackSize)
                        .reversed()
        );

        double incomingX = incoming.getX();
        double incomingZ = incoming.getZ();

        for (Mob target : candidates) {

            if (remaining <= 0) {
                break;
            }

            if (!incoming.isValid()) {
                return;
            }

            if (!canStack(target)) {
                continue;
            }

            /*
            Nearby lookup uses a bounding box.

            Use a squared-distance check for
            the actual horizontal radius.
             */
            double differenceX =
                    incomingX - target.getX();

            double differenceZ =
                    incomingZ - target.getZ();

            double distanceSquared =
                    differenceX * differenceX
                            + differenceZ * differenceZ;

            if (distanceSquared > stackRadiusSquared) {
                continue;
            }

            Island targetIsland = accessService
                    .getIslandAt(target.getLocation())
                    .orElse(null);

            if (targetIsland == null
                    || targetIsland.getIslandId() != island.getIslandId()) {
                continue;
            }

            if (!isCompatible(
                    incoming,
                    target
            )) {
                continue;
            }

            long targetSize =
                    getStackSize(target);

            if (targetSize >= MAX_STACK_SIZE) {
                continue;
            }

            long available =
                    MAX_STACK_SIZE - targetSize;

            long transfer = Math.min(
                    remaining,
                    available
            );

            if (transfer <= 0) {
                continue;
            }

            setStackSize(
                    target,
                    targetSize + transfer
            );

            remaining -= transfer;
        }

        /*
        The whole incoming stack
        was absorbed.
         */
        if (remaining <= 0) {

            queuedMobs.remove(
                    incoming.getUniqueId()
            );

            incoming.remove();

            return;
        }

        /*
        Keep whatever could not fit.
         */
        setStackSize(
                incoming,
                remaining
        );
    }

    /*
    Returns how many logical mobs this
    physical mob represents.
     */
    public long getStackSize(Mob mob) {

        if (stackSizeKey == null) {
            return 1L;
        }

        Long amount = mob.getPersistentDataContainer()
                .get(
                        stackSizeKey,
                        PersistentDataType.LONG
                );

        if (amount == null) {
            return 1L;
        }

        return Math.max(
                1L,
                amount
        );
    }

    public boolean isStacked(Mob mob) {
        return getStackSize(mob) > 1;
    }

    /*
    Sets the logical amount represented
    by this physical mob.

    Even x1 mobs receive stack data and
    display the configured hologram.
     */
    public void setStackSize(
            Mob mob,
            long amount
    ) {

        if (stackSizeKey == null) {
            throw new IllegalStateException(
                    "MobStackService has not been enabled."
            );
        }

        if (amount < 1 || amount > MAX_STACK_SIZE) {
            throw new IllegalArgumentException(
                    "Invalid mob stack size: "
                            + amount
            );
        }

        PersistentDataContainer container =
                mob.getPersistentDataContainer();

        /*
        Store x1 as well.

        This is important because our x1
        custom name must still be recognized
        as belonging to this stacker.
         */
        container.set(
                stackSizeKey,
                PersistentDataType.LONG,
                amount
        );

        refreshDisplay(mob);
    }

    /*
    Adds mobs to an existing stack.

    Returns any logical mobs
    that could not fit.
     */
    public long addToStack(
            Mob mob,
            long amount
    ) {

        if (amount <= 0) {
            return 0L;
        }

        long current =
                getStackSize(mob);

        long available =
                MAX_STACK_SIZE - current;

        long accepted = Math.min(
                amount,
                available
        );

        if (accepted > 0) {
            setStackSize(
                    mob,
                    current + accepted
            );
        }

        return amount - accepted;
    }

    /*
    Removes logical mobs from a stack.

    Returns how many logical mobs remain.
     */
    public long removeFromStack(
            Mob mob,
            long amount
    ) {

        if (amount <= 0) {
            return getStackSize(mob);
        }

        long current =
                getStackSize(mob);

        long remaining =
                current - amount;

        if (remaining <= 0) {

            queuedMobs.remove(
                    mob.getUniqueId()
            );

            mob.remove();

            return 0L;
        }

        setStackSize(
                mob,
                remaining
        );

        return remaining;
    }

    /*
    Determines whether this mob is safe
    to collapse into another physical entity.
     */
    private boolean canStack(Mob mob) {

        if (!mob.isValid() || mob.isDead()) {
            return false;
        }

        /*
        Never stack bosses.
         */
        if (mob instanceof Boss) {
            return false;
        }

        /*
        Villagers contain unique state.
         */
        if (mob instanceof AbstractVillager) {
            return false;
        }

        /*
        Do not collapse individually
        owned pets.
         */
        if (mob instanceof Tameable tameable
                && tameable.isTamed()) {
            return false;
        }

        /*
        Leashed mobs remain individual.
         */
        if (mob.isLeashed()) {
            return false;
        }

        /*
        Mounted mobs remain individual.
         */
        if (mob.isInsideVehicle()
                || !mob.getPassengers().isEmpty()) {
            return false;
        }

        /*
        Do not overwrite names belonging
        to another plugin/player.

        Our own hologram is allowed because
        stack data exists.
         */
        if (!hasStackData(mob)
                && mob.customName() != null) {
            return false;
        }

        return true;
    }

    /*
    Prevents mobs with meaningfully different
    states from being combined.
     */
    private boolean isCompatible(
            Mob first,
            Mob second
    ) {

        if (first.getType() != second.getType()) {
            return false;
        }

        /*
        Do not mix adults and babies.
         */
        if (first instanceof Ageable firstAge
                && second instanceof Ageable secondAge
                && firstAge.isAdult() != secondAge.isAdult()) {

            return false;
        }

        /*
        Slime size changes combat and drops.
         */
        if (first instanceof Slime firstSlime
                && second instanceof Slime secondSlime
                && firstSlime.getSize() != secondSlime.getSize()) {

            return false;
        }

        /*
        Preserve sheep state.
         */
        if (first instanceof Sheep firstSheep
                && second instanceof Sheep secondSheep) {

            if (firstSheep.getColor()
                    != secondSheep.getColor()) {

                return false;
            }

            if (firstSheep.isSheared()
                    != secondSheep.isSheared()) {

                return false;
            }
        }

        /*
        Preserve mooshroom variant.
         */
        if (first instanceof MushroomCow firstCow
                && second instanceof MushroomCow secondCow
                && firstCow.getVariant()
                != secondCow.getVariant()) {

            return false;
        }

        /*
        Do not combine powered and
        normal creepers.
         */
        if (first instanceof Creeper firstCreeper
                && second instanceof Creeper secondCreeper
                && firstCreeper.isPowered()
                != secondCreeper.isPowered()) {

            return false;
        }

        /*
        Equipment must match so drops
        remain correct.
         */
        return hasSameEquipment(
                first,
                second
        );
    }

    private boolean hasSameEquipment(
            Mob first,
            Mob second
    ) {

        EntityEquipment firstEquipment =
                first.getEquipment();

        EntityEquipment secondEquipment =
                second.getEquipment();

        if (firstEquipment == null
                || secondEquipment == null) {

            return firstEquipment == secondEquipment;
        }

        if (!firstEquipment
                .getItemInMainHand()
                .equals(
                        secondEquipment.getItemInMainHand()
                )) {

            return false;
        }

        if (!firstEquipment
                .getItemInOffHand()
                .equals(
                        secondEquipment.getItemInOffHand()
                )) {

            return false;
        }

        return Arrays.equals(
                firstEquipment.getArmorContents(),
                secondEquipment.getArmorContents()
        );
    }

    /*
    Clears only the metadata and visible name
    owned by this stacker.

    Used when the final logical mob dies.
     */
    private void clearStackData(Mob mob) {

        if (stackSizeKey != null) {
            mob.getPersistentDataContainer()
                    .remove(stackSizeKey);
        }

        mob.customName(null);
        mob.setCustomNameVisible(false);
    }

    private boolean hasStackData(Mob mob) {

        return stackSizeKey != null
                && mob.getPersistentDataContainer()
                .has(
                        stackSizeKey,
                        PersistentDataType.LONG
                );
    }

    /*
    Updates the visible stack hologram.

    x1 is intentionally displayed.
     */
    private void refreshDisplay(Mob mob) {

        long amount =
                getStackSize(mob);

        Component name = plugin
                .getMessageService()
                .parse(
                        stackName,

                        Placeholder.unparsed(
                                "amount",
                                Long.toString(amount)
                        ),

                        Placeholder.component(
                                "mob",
                                Component.translatable(
                                        mob.getType()
                                                .translationKey()
                                )
                        )
                );

        mob.customName(name);
        mob.setCustomNameVisible(true);
    }

    /*
    One-time startup pass.

    Existing mobs are only queued here.
    Actual nearby scans are distributed
    over future processing passes.
     */
    private void queueLoadedMobs() {

        bootstrapTask = null;

        if (!enabled) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {

            if (!accessService.isIslandWorld(world)) {
                continue;
            }

            for (Mob mob : world.getEntitiesByClass(Mob.class)) {

                if (!mob.isValid()) {
                    continue;
                }

                queueStack(mob);
            }
        }
    }
}
