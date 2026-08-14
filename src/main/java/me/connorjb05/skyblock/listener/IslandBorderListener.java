package me.connorjb05.skyblock.listener;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerBorderData;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.region.IslandRegion;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import me.connorjb05.skyblock.service.island.IslandUpgradeService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class IslandBorderListener implements Listener {

    private final Skyblock plugin;

    private final IslandAccessService accessService;
    private final IslandUpgradeService upgradeService;
    private final IslandRegionProvider regionProvider;

    private final boolean enabled;

    public IslandBorderListener(
            Skyblock plugin,
            IslandAccessService accessService,
            IslandUpgradeService upgradeService,
            IslandRegionProvider regionProvider
    ) {
        this.plugin = plugin;
        this.accessService = accessService;
        this.upgradeService = upgradeService;
        this.regionProvider = regionProvider;

        //IslandBorderSettings settings = plugin
        //        .getConfigService()
         ///       .getIslandBorderSettings();

        enabled = true;
    }

    /*
    Applies the correct border
    when a player joins.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerJoinEvent(PlayerJoinEvent e) {

        Player player = e.getPlayer();

        /*
        Wait one tick so PlayerData and the
        player's final join location are ready.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            refresh(
                                    player,
                                    player.getLocation()
                            );
                        }
                );
    }

    /*
    Applies the correct border after
    a successful teleport finishes.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerTeleportEvent(PlayerTeleportEvent e) {

        Player player = e.getPlayer();

        /*
        Do NOT apply the border inside
        PlayerTeleportEvent itself.

        Wait until the teleport has completed,
        then use the player's actual location.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            refresh(
                                    player,
                                    player.getLocation()
                            );
                        }
                );
    }

    private void refresh(
            Player player,
            Location location
    ) {

        PlayerData playerData = plugin
                .getPlayerDataService()
                .get(player.getUniqueId());

        if (playerData == null) {
            return;
        }

        PlayerBorderData borderData =
                playerData.getBorderData();

        /*
        Border system disabled.
         */
        if (!enabled) {

            if (borderData.hasBorder()) {
                borderData.clear(player);
            }

            return;
        }

        /*
        Player is not inside an
        island world.
         */
        if (location.getWorld() == null ||
                !accessService.isIslandWorld(location.getWorld())) {

            if (borderData.hasBorder()) {
                borderData.clear(player);
            }

            return;
        }

        /*
        Resolve the island the player is
        physically standing on.
         */
        Island island = accessService
                .getIslandAt(location)
                .orElse(null);

        /*
        Player is inside the island world,
        but not inside an active island.
         */
        if (island == null) {

            if (borderData.hasBorder()) {
                borderData.clear(player);
            }

            return;
        }

        IslandRegion region = regionProvider
                .getRegion(
                        island.getSlotId()
                );

        borderData.apply(
                player,
                island,
                region,
                upgradeService
                        .getIslandSize(island)
        );
    }
}