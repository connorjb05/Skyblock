package me.connorjb05.skyblock.service.island;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.repository.*;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.IslandCache;
import me.connorjb05.skyblock.model.island.IslandDeletion;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.permission.IslandPermissionEntry;
import me.connorjb05.skyblock.model.island.region.GridIslandRegionProvider;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import me.connorjb05.skyblock.model.island.settings.IslandSettingEntry;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeEntry;
import me.connorjb05.skyblock.service.MobStackService;
import me.connorjb05.skyblock.service.stacker.DroppedItemStackerService;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.service.config.settings.IslandRegionSettings;
import me.connorjb05.skyblock.service.island.creation.IslandCreationService;
import me.connorjb05.skyblock.service.island.creation.schematic.FaweIslandSchematicService;
import me.connorjb05.skyblock.service.island.creation.schematic.IslandSchematicService;
import me.connorjb05.skyblock.service.island.deletion.IslandDeletionService;
import me.connorjb05.skyblock.service.island.deletion.wiper.FaweIslandWiper;
import me.connorjb05.skyblock.service.island.deletion.wiper.IslandWiper;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import me.connorjb05.skyblock.service.island.permission.IslandPermissionService;
import me.connorjb05.skyblock.service.island.setting.IslandSettingsService;
import me.connorjb05.skyblock.service.island.teleport.IslandTeleportService;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class IslandService implements Service {

    private final IslandSlotService slotService = new IslandSlotService();

    private IslandRepository islandRepository;
    private IslandUpgradeRepository islandUpgradeRepository;
    private IslandPermissionRepository islandPermissionRepository;
    private IslandSettingsRepository islandSettingsRepository;
    private IslandDeletionRepository islandDeletionRepository;
    private IslandPlayerRepository islandPlayerRepository;

    private final IslandCache cache = new IslandCache();

    private IslandRegionProvider regionProvider;
    private IslandCreationService creationService;
    private IslandDeletionService deletionService;
    private IslandUpgradeService upgradeService;
    private IslandSchematicService schematicService;
    private IslandTeleportService teleportService;
    private IslandAccessService accessService;
    private IslandMembershipService membershipService;
    private IslandPermissionService permissionService;
    private IslandSettingsService settingsService;

    private MobStackService mobStackService;
    private DroppedItemStackerService droppedItemStackService;

    @Override
    public void onEnable(Skyblock plugin) {

        IslandRegionSettings regionSettings = plugin.getConfigService().getIslandRegionSettings();
        regionProvider = new GridIslandRegionProvider(
                regionSettings.world(),
                regionSettings.size(),
                regionSettings.spacing(),
                regionSettings.slotsPerRow(),
                regionSettings.originX(),
                regionSettings.originZ()
        );

        // Set up the repositories based on what type of database the plugin is using.
        islandRepository =
                new IslandRepository(
                        plugin.getDatabase()
                );

        islandUpgradeRepository =
                new IslandUpgradeRepository(
                        plugin.getDatabase()
                );

        islandPermissionRepository =
                new IslandPermissionRepository(
                        plugin.getDatabase()
                );

        islandSettingsRepository =
                new IslandSettingsRepository(
                        plugin.getDatabase()
                );

        islandDeletionRepository =
                new IslandDeletionRepository(
                        plugin.getDatabase()
                );

        islandPlayerRepository =
                new IslandPlayerRepository(
                        plugin.getDatabase()
                );

        upgradeService = new IslandUpgradeService(
                islandUpgradeRepository,
                plugin.getConfigService()
                        .getIslandUpgradeSettings()
        );

        CompletableFuture<List<Island>> islandsFuture =
                islandRepository.findAll();

        CompletableFuture<List<IslandUpgradeEntry>> upgradesFuture =
                islandUpgradeRepository.findAll();

        CompletableFuture<List<IslandMember>> membersFuture =
                islandPlayerRepository.findAll();

        CompletableFuture<List<IslandPermissionEntry>> permissionsFuture =
                islandPermissionRepository.findAll();

        CompletableFuture<List<IslandSettingEntry>> settingsFuture =
                islandSettingsRepository.findAll();

        CompletableFuture<List<IslandDeletion>> deletionsFuture =
                islandDeletionRepository.findAll();

        CompletableFuture.allOf(
                islandsFuture,
                upgradesFuture,
                membersFuture,
                permissionsFuture,
                settingsFuture,
                deletionsFuture
        ).join();

        List<Island> islands = islandsFuture.join();
        List<IslandUpgradeEntry> upgrades = upgradesFuture.join();
        List<IslandMember> members = membersFuture.join();
        List<IslandPermissionEntry> permissions = permissionsFuture.join();
        List<IslandSettingEntry> settings = settingsFuture.join();
        List<IslandDeletion> deletions = deletionsFuture.join();

        /*
        Cache all ACTIVE islands.

        Upgrade data must be applied to the cached
        island objects.
         */
        cache.populate(islands);

        /*
        Apply all island members to
        cached island objects.
         */
        populateMembers(members);

        /*
        Apply only the upgrade rows
        that exist in the database.

        Islands without upgrade rows
        remain at level 0.
         */
        populateUpgrades(upgrades);

        /*
        Apply customized permissions
        to cached islands.
         */
        populatePermissions(permissions);

        /*
        Apply overridden settings
        to cached islands.
         */
        populateSettings(settings);

        /*
        Populate the slot service, this must finish
        BEFORE islands are allowed to be created.
         */
        populateSlots(
                islands,
                deletions
        );

        schematicService = new FaweIslandSchematicService(
                plugin,
                regionProvider,
                // TODO make this configurable maybe?
                new File(plugin.getDataFolder() + "/schematics", "island.schem"),
                100,
                0.5,
                1,
                0.5
        );

        teleportService = new IslandTeleportService(
                plugin,
                regionProvider,
                100,
                0.5,
                1,
                0.5
        );

        IslandWiper islandWiper = new FaweIslandWiper(
                plugin,
                regionProvider,
                () -> {
                    World world = Bukkit.getWorld("world");

                    if (world == null) {
                        return null;
                    }

                    return world.getSpawnLocation();
                }
        );

        deletionService = new IslandDeletionService(
                slotService,
                islandRepository,
                islandDeletionRepository,
                islandPlayerRepository,
                islandWiper,
                cache
        );

        creationService = new IslandCreationService(
                slotService,
                schematicService,
                teleportService,
                deletionService,
                islandRepository,
                islandPlayerRepository,
                cache
        );

        accessService = new IslandAccessService(
                regionProvider,
                cache
        );

        membershipService = new IslandMembershipService(
                islandPlayerRepository,
                cache
        );

        permissionService = new IslandPermissionService(
                plugin,
                islandPermissionRepository,
                accessService
        );

        settingsService = new IslandSettingsService(
                islandSettingsRepository
        );

        deletionService.populate(deletions);
        deletionService.start(plugin);

        mobStackService = new MobStackService(accessService);
        mobStackService.onEnable(plugin);

        droppedItemStackService = new DroppedItemStackerService(accessService);
        droppedItemStackService.onEnable(plugin);
    }

    @Override
    public void onDisable(Skyblock plugin) {

        if (mobStackService != null) {
            mobStackService.onDisable(plugin);
        }

        if (droppedItemStackService != null) {
            droppedItemStackService.onDisable(plugin);
        }

        if (deletionService != null) {
            deletionService.stop();
        }

    }

    public CompletableFuture<Void> loadPlayer(PlayerData playerData) {
        return islandPlayerRepository
                .find(playerData.getUuid())
                .thenAccept(optional -> {

                    if (optional.isEmpty()) {
                        playerData.setIslandId(-1L);
                        playerData.setIslandMember(null);
                        return;
                    }

                    IslandMember islandMember = optional.get();

                    playerData.setIslandId(islandMember.islandId());
                    playerData.setIslandMember(islandMember);

                });
    }

    private void populateMembers(List<IslandMember> members) {

        for (IslandMember member : members) {

            Island island = getIsland(member.islandId())
                    .orElse(null);

            /*
            If island is null then the member row
            references an island that is not
            in the active cache.

            Normally the foreign key/cascade
            should prevent this.
             */
            if (island == null) {
                continue;
            }

            island.setMember(member);
        }

    }

    private void populatePermissions(List<IslandPermissionEntry> permissions) {

        for (IslandPermissionEntry entry : permissions) {

            Island island = getIsland(entry.islandId())
                    .orElse(null);

            /*
            If island is null then the permission
            row references an island that is no
            longer in the active island cache.
             */
            if (island == null) {
                continue;
            }

            /*
            Default permission levels should
            never be stored as overrides.

            Pointless memory usage.
            Ignore them if one somehow exists.
            DB should take care of this.
             */
            if (entry.permissionLevel() == entry.permission().getDefaultPermissionLevel()) {
                continue;
            }

            island.getPermissionData().setPermissionLevel(
                    entry.permission(),
                    entry.permissionLevel()
            );
        }

    }

    private void populateSettings(List<IslandSettingEntry> settings) {

        for (IslandSettingEntry entry : settings) {

            Island island = getIsland(entry.islandId())
                    .orElse(null);

            if (island == null) {
                continue;
            }

            island.getSettingsData().setValue(
                    entry.setting(),
                    entry.value()
            );
        }

    }

    private void populateUpgrades(List<IslandUpgradeEntry> upgrades) {

        for (IslandUpgradeEntry entry : upgrades) {

            Island island = getIsland(entry.islandId())
                    .orElse(null);

            /*
            If island is null then the upgrade row
            is referencing an island that is no longer
            in the active cache

            Normally the foreign key/cascade should
            prevent this, but this will reinforce the cascade.
             */
            if (island == null) {
                continue;
            }

            island.getUpgradeData().setLevel(
                    entry.type(),
                    entry.level()
            );
        }

    }

    private void populateSlots(
            List<Island> islands,
            List<IslandDeletion> deletions
    ) {

        Set<Long> occupiedSlots =
                new HashSet<>();

        /*
         * Active islands.
         */
        islands.stream()
                .map(Island::getSlotId)
                .forEach(
                        occupiedSlots::add
                );

        /*
         * Islands waiting to be physically wiped.
         *
         * Their slots must remain hostage.
         */
        deletions.stream()
                .map(IslandDeletion::getSlotId)
                .forEach(
                        occupiedSlots::add
                );

        slotService.populate(
                occupiedSlots
        );
    }

    public Optional<Island> getIsland(long islandId) {
        return cache.findById(islandId);
    }

    public Optional<Island> getIslandBySlot(long slotId) {
        return cache.findBySlotId(slotId);
    }

    public IslandRegionProvider getRegionProvider() {
        return regionProvider;
    }

    public IslandSlotService getSlotService() {
        return slotService;
    }

    public IslandAccessService getAccessService() {
        return accessService;
    }

    public IslandPermissionService getPermissionService() {
        return permissionService;
    }

    public IslandSettingsService getSettingsService() {
        return settingsService;
    }

    public IslandTeleportService getTeleportService() {
        return teleportService;
    }

    public IslandMembershipService getMembershipService() {
        return membershipService;
    }

    public IslandCreationService getCreationService() {
        return creationService;
    }

    public IslandUpgradeService getUpgradeService() {
        return upgradeService;
    }

    public IslandDeletionService getDeletionService() {
        return deletionService;
    }
}
