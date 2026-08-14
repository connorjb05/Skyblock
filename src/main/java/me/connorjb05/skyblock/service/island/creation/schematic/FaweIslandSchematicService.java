package me.connorjb05.skyblock.service.island.creation.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.region.IslandRegion;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class FaweIslandSchematicService
        implements IslandSchematicService {

    private final Skyblock plugin;

    private final IslandRegionProvider regionProvider;

    private final Clipboard clipboard;

    private final int pasteY;

    private final double spawnOffsetX;
    private final double spawnOffsetY;
    private final double spawnOffsetZ;

    public FaweIslandSchematicService(
            Skyblock plugin,
            IslandRegionProvider regionProvider,
            File schematicFile,
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

        this.clipboard = loadSchematic(schematicFile);
    }

    @Override
    public CompletableFuture<IslandPasteResult> paste(
            Island island
    ) {

        IslandRegion region = regionProvider.getRegion(
                island.getSlotId()
        );

        return resolvePaste(region)
                .thenCompose(this::pasteSchematic);
    }

    /*
     Resolves the physical origin of the
     island slot.
     *
     This origin becomes the target position
     for the schematic's saved copy position.
     */
    private CompletableFuture<ResolvedPaste> resolvePaste(IslandRegion region) {

        CompletableFuture<ResolvedPaste> future =
                new CompletableFuture<>();

        Runnable task = () -> {

            try {

                World world = Bukkit.getWorld(region.worldName());

                if (world == null) {
                    throw new IllegalStateException(
                            "Island world is not loaded: "
                                    + region.worldName()
                    );
                }

                /*
                 IMPORTANT:

                 Do NOT modify the clipboard origin.

                 WorldEdit remembers where the player
                 was relative to the schematic when it
                 was originally copied.

                 We simply tell WorldEdit that this
                 position is now the equivalent point
                 for this island slot.
                 */
                BlockVector3 pasteOrigin = BlockVector3.at(
                        region.originX(),
                        pasteY,
                        region.originZ()
                );

                /*
                 Player spawn can be configured
                 relative to the same slot origin.
                 */
                Location spawnLocation = new Location(
                        world,
                        region.originX() + spawnOffsetX,
                        pasteY + spawnOffsetY,
                        region.originZ() + spawnOffsetZ
                );

                future.complete(new ResolvedPaste(
                                world,
                                pasteOrigin,
                                spawnLocation
                        )
                );

            } catch (Throwable throwable) {

                future.completeExceptionally(throwable);
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
     Physically pastes the schematic.
     */
    private CompletableFuture<IslandPasteResult> pasteSchematic(
            ResolvedPaste resolved
    ) {

        CompletableFuture<IslandPasteResult> future =
                new CompletableFuture<>();

        Bukkit.getScheduler()
                .runTaskAsynchronously(plugin, () -> {

                            try {

                                com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(
                                        resolved.world()
                                );

                                try (
                                        EditSession editSession = WorldEdit
                                                .getInstance()
                                                .newEditSessionBuilder()
                                                .world(world)
                                                .fastMode(true)
                                                .limitUnlimited()
                                                .allowedRegionsEverywhere()
                                                .changeSetNull()
                                                .build()
                                ) {

                                    /*
                                    The schematic's saved relative
                                    copy position is preserved here.

                                    .to(...) specifies where that
                                    copied position should now be.
                                     */
                                    Operation operation = new ClipboardHolder(clipboard)
                                            .createPaste(editSession)
                                            .to(resolved.pasteOrigin())
                                            .build();

                                    Operations.complete(operation);
                                }

                                plugin.getLogger()
                                        .info(
                                                "Pasted island schematic at "
                                                        + resolved.pasteOrigin()
                                        );

                                future.complete(new IslandPasteResult(
                                        resolved.spawnLocation())
                                );

                            } catch (Throwable throwable) {

                                future.completeExceptionally(throwable);
                            }
                        }
                );

        return future;
    }

    /*
     Loads the starter island schematic
     once when this service is created.
     */
    private Clipboard loadSchematic(File file) {

        if (!file.exists()) {
            throw new IllegalStateException(
                    "Island schematic does not exist: "
                            + file.getAbsolutePath()
            );
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);

        if (format == null) {
            throw new IllegalStateException(
                    "Unknown island schematic format: "
                            + file.getName()
            );
        }

        try (
                FileInputStream inputStream = new FileInputStream(file);

                ClipboardReader reader = format.getReader(inputStream)
        ) {

            return reader.read();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load island schematic: "
                            + file.getName(),
                    exception
            );
        }
    }

    private record ResolvedPaste(
            World world,
            BlockVector3 pasteOrigin,
            Location spawnLocation
    ) {
    }
}