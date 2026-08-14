package me.connorjb05.skyblock.model;

import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.region.IslandRegion;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

public class PlayerBorderData {

    private WorldBorder border;

    /*
    Island the player is
    physically visiting.
     */
    private long islandId = -1L;

    private String worldName;

    /*
    Current visible island size.
     */
    private double size;

    public boolean hasBorder() {
        return border != null && islandId >= 0;
    }

    public boolean isViewingIsland(long islandId) {
        return this.islandId == islandId;
    }

    /*
    Applies the island's border
    to this player.
     */
    public void apply(
            Player player,
            Island island,
            IslandRegion region,
            double requestedSize
    ) {

        apply(
                player,
                island,
                region,
                requestedSize,
                0L
        );
    }

    /*
    Applies or updates the island border.

    The border is aligned to whole block
    boundaries around the island origin.

    This prevents the world border from
    cutting through blocks when using
    odd sizes such as 25, 51, etc.
     */
    public void apply(
            Player player,
            Island island,
            IslandRegion region,
            double requestedSize,
            long transitionTicks
    ) {

        /*
        Physical dimensions of the slot.
         */
        int physicalWidth =
                region.maxX()
                        - region.minX()
                        + 1;

        int physicalDepth =
                region.maxZ()
                        - region.minZ()
                        + 1;

        int maximumSize = Math.min(
                physicalWidth,
                physicalDepth
        );

        /*
        Island sizes should represent an
        exact number of usable blocks.

        Keep the border on a whole block size
        so its edges can be aligned exactly.
         */
        int newSize = Math.max(
                1,
                Math.min(
                        (int) Math.round(requestedSize),
                        maximumSize
                )
        );

        /*
        The island origin represents the
        central anchor block of the slot.
         */
        int originX =
                region.originX();

        int originZ =
                region.originZ();

        /*
        Calculate the first usable block.

        Using (size - 1) / 2 is important.

        Odd example:
        size = 25
        offset = 12

        Even example:
        size = 24
        offset = 11

        This also means a maximum-size border
        lines back up with the physical slot.
         */
        int borderMinX =
                originX
                        - ((newSize - 1) / 2);

        int borderMinZ =
                originZ
                        - ((newSize - 1) / 2);

        /*
        These are EXCLUSIVE maximum edges.

        The border itself exists between
        blocks rather than through them.
         */
        double borderMaxX =
                borderMinX
                        + newSize;

        double borderMaxZ =
                borderMinZ
                        + newSize;

        /*
        WorldBorder wants a center and size.

        Derive the center from the exact
        block-aligned edges.

        Odd sizes result in .5 centers.
        Even sizes result in whole-number centers.
         */
        double centerX =
                (
                        borderMinX
                                + borderMaxX
                ) / 2.0D;

        double centerZ =
                (
                        borderMinZ
                                + borderMaxZ
                ) / 2.0D;

        /*
        Create the player's virtual border
        the first time they need one.
         */
        if (border == null) {

            border = Bukkit
                    .getServer()
                    .createWorldBorder();

            border.setWarningDistance(
                    0
            );

            border.setWarningTimeTicks(
                    0
            );
        }

        boolean sameIsland =
                islandId == island.getIslandId();

        border.setCenter(
                centerX,
                centerZ
        );

        /*
        Animate an existing island border
        when its size changes.

        Teleporting to another island should
        apply its border immediately.
         */
        if (sameIsland
                && transitionTicks > 0L
                && Double.compare(size, newSize) != 0) {

            border.changeSize(
                    newSize,
                    transitionTicks
            );

        } else {

            border.setSize(
                    newSize
            );
        }

        player.setWorldBorder(
                border
        );

        islandId = island.getIslandId();
        worldName = region.worldName();

        size = newSize;
    }

    /*
    Updates the current island's border size.

    This is useful directly from the
    ISLAND_SIZE upgrade menu.
     */
    public void updateSize(
            Player player,
            double newSize,
            long transitionTicks
    ) {

        if (!hasBorder()) {
            return;
        }

        newSize = Math.max(
                1.0D,
                Math.round(newSize)
        );

        if (transitionTicks > 0L) {

            border.changeSize(
                    newSize,
                    transitionTicks
            );

        } else {

            border.setSize(
                    newSize
            );
        }

        size = newSize;

        player.setWorldBorder(
                border
        );
    }

    /*
    Removes the player's custom border
    and restores the normal world border.
     */
    public void clear(Player player) {

        player.setWorldBorder(
                null
        );

        border = null;

        islandId = -1L;
        worldName = null;

        size = 0.0D;
    }

    public WorldBorder getBorder() {
        return border;
    }

    public long getIslandId() {
        return islandId;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getSize() {
        return size;
    }
}