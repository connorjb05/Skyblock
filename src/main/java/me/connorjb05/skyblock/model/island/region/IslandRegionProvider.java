package me.connorjb05.skyblock.model.island.region;

import java.util.OptionalLong;

public interface IslandRegionProvider {
    IslandRegion getRegion(long slotId);

    OptionalLong getSlotIdAt(
            String worldName,
            int x,
            int z
    );

    boolean isIslandWorld(String worldName);
}
