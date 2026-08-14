package me.connorjb05.skyblock.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandRespawnListener implements Listener {

    private final Skyblock plugin;
    private final IslandAccessService accessService;

    private final Set<UUID> islandRespawns = new HashSet<>();

    public IslandRespawnListener(
            Skyblock plugin,
            IslandAccessService accessService
    ) {
        this.plugin = plugin;
        this.accessService = accessService;
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerDeathEvent(PlayerDeathEvent e) {

        Player player = e.getPlayer();

        /*
        Keep inventory ONLY when dying
        inside one of the Skyblock
        island worlds.
         */
        if (accessService.isIslandWorld(player.getWorld())) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            e.getDrops().clear();
        }

        PlayerData playerData = plugin
                .getPlayerDataService()
                .get(player);

        if (playerData == null || !playerData.hasIsland()) {
            return;
        }

        Island island = accessService
                .getIslandAt(player.getLocation())
                .orElse(null);

        if (island == null) {
            return;
        }

        /*
        Only respawn at personal island home
        IF they died on their island, otherwise
        send back to spawn.
         */
        if (playerData.getIslandId() != island.getIslandId()) {
            return;
        }

        islandRespawns.add(player.getUniqueId());
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerPostRespawnEvent(PlayerPostRespawnEvent e) {

        Player player = e.getPlayer();

        if (!islandRespawns.remove(player.getUniqueId())) {
            return;
        }

        PlayerData playerData = plugin
                .getPlayerDataService()
                .get(player);

        if (playerData == null || !playerData.hasIsland()) {
            return;
        }

        Island island = plugin
                .getIslandService()
                .getIsland(playerData.getIslandId())
                .orElse(null);

        if (island == null) {
            return;
        }

        plugin.getIslandService()
                .getTeleportService()
                .teleport(
                        player.getUniqueId(),
                        island
                );
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        islandRespawns.remove(e.getPlayer().getUniqueId());
    }
}
