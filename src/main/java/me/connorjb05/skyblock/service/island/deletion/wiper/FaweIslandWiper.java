package me.connorjb05.skyblock.service.island.deletion.wiper;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.IslandDeletion;
import me.connorjb05.skyblock.model.island.region.IslandRegion;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FaweIslandWiper implements IslandWiper {

    private final Skyblock plugin;

    private final IslandRegionProvider regionProvider;

    /*
    Where players are sent before their
    island is physically wiped.
     */
    private final Supplier<Location> evacuationLocation;

    public FaweIslandWiper(
            Skyblock plugin,
            IslandRegionProvider regionProvider,
            Supplier<Location> evacuationLocation
    ) {
        this.plugin = plugin;
        this.regionProvider = regionProvider;
        this.evacuationLocation = evacuationLocation;
    }

    @Override
    public CompletableFuture<Void> wipe(IslandDeletion deletion) {

        IslandRegion region = regionProvider.getRegion(deletion.getSlotId());

        /*
        Bukkit player/entity operations
        must happen on the server thread
         */

        return prepareRegion(region)

                /*
                 * FAWE block operation happens
                 * asynchronously.
                 */
                .thenCompose(this::wipeBlocks)

                /*
                 * Clean entities one final time
                 * after the FAWE operation.
                 */
                .thenCompose(this::finishRegion)

                .thenApply(ignored -> null);
    }

    private CompletableFuture<ResolvedRegion> prepareRegion(
            IslandRegion region
    ) {
        CompletableFuture<ResolvedRegion> future =
                new CompletableFuture<>();

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    try {

                        World world =
                                Bukkit.getWorld(
                                        region.worldName()
                                );

                        if (world == null) {
                            throw new IllegalStateException(
                                    "Island world is not loaded: "
                                            + region.worldName()
                            );
                        }

                        com.sk89q.worldedit.world.World worldEditWorld =
                                BukkitAdapter.adapt(
                                        world
                                );

                        ResolvedRegion resolved =
                                new ResolvedRegion(
                                        world,
                                        worldEditWorld,
                                        region
                                );

                        clearEntities(
                                resolved
                        );

                        future.complete(
                                resolved
                        );

                    } catch (Throwable throwable) {

                        future.completeExceptionally(
                                throwable
                        );
                    }
                }
        );

        return future;
    }

    private CompletableFuture<ResolvedRegion> wipeBlocks(
            ResolvedRegion resolved
    ) {
        CompletableFuture<ResolvedRegion> future =
                new CompletableFuture<>();

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                IslandRegion islandRegion =
                                        resolved.region();

                                com.sk89q.worldedit.world.World world =
                                        resolved.worldEditWorld();

                                BlockVector3 minimum =
                                        BlockVector3.at(
                                                islandRegion.minX(),
                                                world.getMinY(),
                                                islandRegion.minZ()
                                        );

                                BlockVector3 maximum =
                                        BlockVector3.at(
                                                islandRegion.maxX(),
                                                world.getMaxY(),
                                                islandRegion.maxZ()
                                        );

                                Region region =
                                        new CuboidRegion(
                                                world,
                                                minimum,
                                                maximum
                                        );

                                long startTime = System.nanoTime();

                                try (
                                        EditSession editSession =
                                                WorldEdit
                                                        .getInstance()
                                                        .newEditSessionBuilder()
                                                        .world(world)
                                                        .fastMode(true)
                                                        .limitUnlimited()
                                                        .allowedRegionsEverywhere()
                                                        .changeSetNull()
                                                        .build()
                                ) {

                                    ExistingBlockMask existingBlockMask =
                                            new ExistingBlockMask(
                                                    editSession
                                            );

                                    int affectedBlocks =
                                            editSession.replaceBlocks(
                                                    region,
                                                    existingBlockMask,
                                                    BlockTypes.AIR
                                            );

                                    /*
                                     * EditSession.close() has now run.
                                     *
                                     * Stop the timer AFTER closing the
                                     * EditSession so flushing is included
                                     * in the benchmark.
                                     *
                                     * Closing an EditSession flushes its
                                     * buffered work.
                                     */
                                    long elapsedNanos =
                                            System.nanoTime()
                                                    - startTime;

                                    double elapsedMilliseconds =
                                            elapsedNanos
                                                    / 1_000_000.0;

                                    double elapsedSeconds =
                                            elapsedNanos
                                                    / 1_000_000_000.0;

                                    plugin.getLogger().info(
                                            "Island wipe completed:"
                                    );

                                    plugin.getLogger().info(
                                            "Region: "
                                                    + islandRegion
                                    );

                                    plugin.getLogger().info(
                                            "Affected blocks: "
                                                    + affectedBlocks
                                    );

                                    plugin.getLogger().info(
                                            String.format(
                                                    "Time: %.2f ms (%.3f seconds)",
                                                    elapsedMilliseconds,
                                                    elapsedSeconds
                                            )
                                    );

                                }

                                /*
                                 * Closing the EditSession above is
                                 * important. FAWE flushes/completes
                                 * the edit through the session lifecycle.
                                 */

                                future.complete(
                                        resolved
                                );

                            } catch (Throwable throwable) {

                                future.completeExceptionally(
                                        throwable
                                );
                            }
                        }
                );

        return future;
    }

    private CompletableFuture<ResolvedRegion> finishRegion(
            ResolvedRegion resolved
    ) {
        CompletableFuture<ResolvedRegion> future =
                new CompletableFuture<>();

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    try {

                        /*
                         * Run entity cleanup once more
                         * after blocks have disappeared.
                         */
                        clearEntities(
                                resolved
                        );

                        future.complete(
                                resolved
                        );

                    } catch (Throwable throwable) {

                        future.completeExceptionally(
                                throwable
                        );
                    }
                }
        );

        return future;
    }

    private void clearEntities(
            ResolvedRegion resolved
    ) {
        World world =
                resolved.bukkitWorld();

        IslandRegion region =
                resolved.region();

        BoundingBox boundingBox =
                new BoundingBox(
                        region.minX(),
                        world.getMinHeight(),
                        region.minZ(),

                        region.maxX() + 1.0,
                        world.getMaxHeight(),
                        region.maxZ() + 1.0
                );

        for (
                Entity entity
                : world.getNearbyEntities(
                boundingBox
        )
        ) {

            /*
             * Never remove a player.
             *
             * Move them somewhere safe first.
             */
            if (entity instanceof Player player) {

                Location target =
                        evacuationLocation.get();

                if (target == null) {
                    throw new IllegalStateException(
                            "Island evacuation location is null."
                    );
                }

                /*
                 * Make sure the evacuation point isn't
                 * inside the region we're deleting.
                 */
                if (
                        target.getWorld() == world
                                && boundingBox.contains(
                                target.toVector()
                        )
                ) {
                    throw new IllegalStateException(
                            "Island evacuation location is inside the deletion region."
                    );
                }

                boolean teleported =
                        player.teleport(
                                target
                        );

                if (!teleported) {
                    throw new IllegalStateException(
                            "Failed to evacuate player "
                                    + player.getName()
                    );
                }

                continue;
            }

            /*
             * Armor stands, dropped items, mobs,
             * minecarts, item frames, displays, etc.
             */
            entity.remove();
        }
    }

    private record ResolvedRegion(
            World bukkitWorld,
            com.sk89q.worldedit.world.World worldEditWorld,
            IslandRegion region
    ) {
    }
}
