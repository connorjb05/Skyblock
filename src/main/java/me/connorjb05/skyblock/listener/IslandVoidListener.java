package me.connorjb05.skyblock.listener;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.service.island.permission.IslandAccessService;
import me.connorjb05.skyblock.util.ExceptionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandVoidListener implements Listener {

    private final Skyblock plugin;
    private final IslandAccessService accessService;

    /*
    Prevents multiple teleportAsync calls
    while a player's destination chunk
    is still being resolved.
     */
    private final Set<UUID> teleporting =
            ConcurrentHashMap.newKeySet();

    public IslandVoidListener(
            Skyblock plugin,
            IslandAccessService accessService
    ) {
        this.plugin = plugin;
        this.accessService = accessService;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onVoidDamageEvent(EntityDamageEvent e) {

        if (e.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        // Only handle the island world.
        if (!accessService.isIslandWorld(player.getWorld())) {
            return;
        }

        e.setCancelled(true);

        player.setFallDistance(0.0F);

        UUID playerId = player.getUniqueId();

        /*
        Don't start another teleport while
        one is already being processed.
         */
        if (!teleporting.add(playerId)) {
            return;
        }

        /*
        Determine which island the player
        physically fell from.

        The Y coordinate does not matter.
        Island lookup is based on X/Z.
         */
        Island island = accessService
                .getIslandAt(player.getLocation())
                .orElse(null);

        if (island != null) {

            teleportToIsland(
                    playerId,
                    island
            );

            return;
        }

        /*
        Player is not over an active island.

        Send them to the normal world spawn.
         */
        teleportToSpawn(
                playerId
        );
    }

    private void teleportToIsland(
            UUID playerId,
            Island island
    ) {

        plugin.getIslandService()
                .getTeleportService()
                .teleport(
                        playerId,
                        island
                )
                .whenComplete(
                        (success, throwable) ->
                                finishTeleport(
                                        playerId,
                                        throwable
                                )
                );
    }

    private void teleportToSpawn(
            UUID playerId
    ) {

        World world = Bukkit.getWorld("world");

        if (world == null) {

            teleporting.remove(playerId);

            plugin.getLogger()
                    .warning(
                            "Could not recover player "
                                    + playerId
                                    + " from the void because world 'world' is not loaded."
                    );

            return;
        }

        Location spawnLocation = world.getSpawnLocation();

        plugin.getIslandService()
                .getTeleportService()
                .teleport(
                        playerId,
                        spawnLocation
                )
                .whenComplete(
                        (success, throwable) ->
                                finishTeleport(
                                        playerId,
                                        throwable
                                )
                );
    }

    private void finishTeleport(
            UUID playerId,
            Throwable throwable
    ) {

        teleporting.remove(playerId);

        if (throwable == null) {
            return;
        }

        Throwable cause = ExceptionUtil.unwrap(throwable);

        plugin.getLogger()
                .warning(
                        "Failed to recover player "
                                + playerId
                                + " from the void: "
                                + cause.getMessage()
                );
    }
}