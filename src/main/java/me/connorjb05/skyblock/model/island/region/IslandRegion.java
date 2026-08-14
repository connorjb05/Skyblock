package me.connorjb05.skyblock.model.island.region;

public record IslandRegion(
        String worldName,
        int minX,
        int maxX,
        int minZ,
        int maxZ
) {

    public int originX() {
        return minX + ((maxX - minX) / 2);
    }

    public int originZ() {
        return minZ + ((maxZ - minZ) / 2);
    }

    public boolean contains(
            int x,
            int z
    ) {
        return x >= minX &&
                x <= maxX &&
                z >= minZ &&
                z <= maxZ;
    }

}
