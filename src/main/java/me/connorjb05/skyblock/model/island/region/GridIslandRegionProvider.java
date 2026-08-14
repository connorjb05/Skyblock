package me.connorjb05.skyblock.model.island.region;

import java.util.OptionalLong;

public class GridIslandRegionProvider implements IslandRegionProvider {

    private final String worldName;

    private final int regionSize;
    private final int spacing;

    private final int slotsPerRow;

    private final int originX;
    private final int originZ;

    public GridIslandRegionProvider(
            String worldName,
            int regionSize,
            int spacing,
            int slotsPerRow,
            int originX,
            int originZ
    ) {

        if (regionSize <= 0) {
            throw new IllegalArgumentException(
                    "Region size must be greater than zero."
            );
        }

        if (spacing < regionSize) {
            throw new IllegalArgumentException(
                    "Island spacing cannot be smaller than region size."
            );
        }

        if (slotsPerRow <= 0) {
            throw new IllegalArgumentException(
                    "Slots per row must be greater than zero."
            );
        }

        this.worldName = worldName;

        this.regionSize = regionSize;
        this.spacing = spacing;

        this.slotsPerRow = slotsPerRow;

        this.originX = originX;
        this.originZ = originZ;
    }

    @Override
    public IslandRegion getRegion(long slotId) {

        if (slotId < 0) {
            throw new IllegalArgumentException(
                    "Slot ID cannot be negative."
            );
        }

        long column = slotId % slotsPerRow;
        long row = slotId / slotsPerRow;

        long centerX = originX + (column * spacing);
        long centerZ = originZ + (row * spacing);

        int x = Math.toIntExact(centerX);
        int z = Math.toIntExact(centerZ);

        int half = regionSize / 2;

        int minX = x - half;
        int minZ = z - half;

        int maxX = minX + regionSize - 1;
        int maxZ = minZ + regionSize - 1;

        return new IslandRegion(
                worldName,
                minX,
                maxX,
                minZ,
                maxZ
        );
    }

    /**
     * Constant-time reverse lookup.
     *
     * Instead of scanning every island,
     * determine which grid center is nearest
     * to this position and verify that the
     * position is inside that slot's region.
     *
     * @param worldName name of the world
     * @param x block x coordinate
     * @param z block z coordinate
     * @return nearest slot id
     */
    @Override
    public OptionalLong getSlotIdAt(
            String worldName,
            int x,
            int z
    ) {

        if (!isIslandWorld(worldName)) {
            return OptionalLong.empty();
        }

        long column = Math.round((x - (double) originX) / spacing);

        long row = Math.round((z - (double) originZ) / spacing);

        if (column < 0 || column >= slotsPerRow || row < 0) {
            return OptionalLong.empty();
        }

        final long slotId;

        try {
            slotId = Math.addExact(
                    Math.multiplyExact(row, (long) slotsPerRow),
                    column
            );
        } catch (ArithmeticException exception) {
            return OptionalLong.empty();
        }

        IslandRegion region = getRegion(slotId);

        /*
        Important because there can be empty
        space between neighboring slots.
         */
        if (!region.contains(x, z)) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(slotId);
    }

    @Override
    public boolean isIslandWorld(String worldName) {
        return this.worldName.equals(worldName);
    }
}
