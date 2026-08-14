package me.connorjb05.skyblock.service.island.teleport;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.region.IslandRegion;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandTeleportService {

    private final Skyblock plugin;

    private final IslandRegionProvider regionProvider;

    private final int pasteY;

    private final double spawnOffsetX;
    private final double spawnOffsetY;
    private final double spawnOffsetZ;

    public IslandTeleportService(
            Skyblock plugin,
            IslandRegionProvider regionProvider,
            int pasteY,
            double spawnOffsetX,
            double spawnOffsetY,
            double spawnOffsetZ
    ) {
        this.plugin = plugin;
        this.regionProvider = regionProvider;

        this.pasteY = pasteY;

        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetY = spawnOffsetY;
        this.spawnOffsetZ = spawnOffsetZ;
    }

    /*
     * Teleports a player to an island's
     * configured home location.
     */
    public CompletableFuture<Boolean> teleport(
            UUID playerId,
            Island island
    ) {

        CompletableFuture<Boolean> future =
                new CompletableFuture<>();

        Runnable task = () -> {

            try {

                Player player = Bukkit.getPlayer(playerId);

                if (player == null || !player.isOnline()) {
                    future.complete(false);
                    return;
                }

                /*
                Clear any existing fall distance
                to negate fall damage before
                teleporting to the island.
                 */
                player.setFallDistance(0.0F);

                IslandRegion region = regionProvider.getRegion(island.getSlotId());

                World world = Bukkit.getWorld(region.worldName());

                if (world == null) {
                    throw new IllegalStateException(
                            "Island world is not loaded: "
                                    + region.worldName()
                    );
                }

                Location location = new Location(
                        world,
                        region.originX() + spawnOffsetX,
                        pasteY + spawnOffsetY,
                        region.originZ() + spawnOffsetZ
                );

                teleport(
                        playerId,
                        location
                ).whenComplete(
                        (success, throwable) -> {

                            if (throwable != null) {
                                future.completeExceptionally(throwable);
                                return;
                            }

                            if (!success) {
                                future.complete(false);
                                return;
                            }

                            /*
                            Run island-specific actions
                            after the teleport completes.
                             */
                            Bukkit.getScheduler().runTask(
                                    plugin,
                                    () -> {

                                        Player teleportedPlayer = Bukkit.getPlayer(playerId);

                                        if (teleportedPlayer == null
                                                || !teleportedPlayer.isOnline()) {

                                            future.complete(false);
                                            return;
                                        }

                                        /*
                                        Clear fall distance again
                                        after arriving at the island.
                                         */
                                        teleportedPlayer.setFallDistance(0.0F);

                                        sendTeleportMessage(
                                                teleportedPlayer,
                                                island
                                        );

                                        future.complete(true);
                                    }
                            );
                        }
                );

            } catch (Throwable throwable) {

                future.completeExceptionally(
                        throwable
                );
            }
        };

        if (Bukkit.isPrimaryThread()) {

            task.run();

        } else {

            Bukkit.getScheduler().runTask(plugin, task);
        }

        return future;
    }

    /*
     * Teleports a player to a specific
     * Bukkit location.
     */
    public CompletableFuture<Boolean> teleport(
            UUID playerId,
            Location location
    ) {

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Runnable task = () -> {

            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline()) {
                future.complete(false);
                return;
            }

            player.teleportAsync(location)
                    .whenComplete(
                            (success, throwable) -> {

                                if (throwable != null) {

                                    future.completeExceptionally(throwable);

                                    return;
                                }

                                future.complete(success);
                            }
                    );
        };

        if (Bukkit.isPrimaryThread()) {

            task.run();

        } else {

            Bukkit.getScheduler().runTask(plugin, task);
        }

        return future;
    }

    /*
    Sends the island teleport
    message and title.
     */
    private void sendTeleportMessage(
            Player player,
            Island island
    ) {

        String ownerName = getOwnerName(island);

        TagResolver ownerResolver =
                Placeholder.unparsed(
                        "island_owner",
                        ownerName
                );

        IslandTeleportStatus.SUCCESS.sendMessage(
                player,
                ownerResolver
        );

        IslandTeleportStatus.TITLE.sendTitle(
                player,
                IslandTeleportStatus.SUBTITLE,
                ownerResolver
        );
    }

    /*
    Gets the display name of
    the island's owner.
     */
    private String getOwnerName(
            Island island
    ) {

        Player owner = Bukkit.getPlayer(island.getLeaderId());

        if (owner != null) {
            return owner.getName();
        }

        String leaderName = Bukkit.getOfflinePlayer(island.getLeaderId())
                .getName();

        if (leaderName == null) {
            return "Unknown";
        }

        return leaderName;
    }
}