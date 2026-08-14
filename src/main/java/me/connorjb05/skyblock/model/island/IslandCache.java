package me.connorjb05.skyblock.model.island;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache for active islands.
 *
 * The database remains the persistent source of truth,
 * while this cache is used for fast runtime lookups.
 */
public class IslandCache {

    private final ConcurrentMap<Long, Island> islandsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Island> islandsBySlot = new ConcurrentHashMap<>();

    /**
     * Populates the cache with islands loaded
     * from persistent storage.
     *
     * @param islands islands to cache
     */
    public synchronized void populate(Collection<Island> islands) {

        if (islands == null) {
            throw new IllegalArgumentException(
                    "Islands cannot be null."
            );
        }

        clear();

        for (Island island : islands) {
            add(island);
        }
    }

    /**
     * Adds an island to the cache.
     *
     * @param island island to cache
     */
    public synchronized void add(Island island) {

        Island previous = islandsById.put(
                island.getIslandId(),
                island
        );

        /*
        Protect against a stale slot mapping
        if an island instance is replaced.
         */
        if (previous != null && previous.getSlotId() != island.getSlotId()) {
            islandsBySlot.remove(previous.getSlotId(), previous);
        }

        islandsBySlot.put(
                island.getSlotId(),
                island
        );

    }


    /**
     * Removes an island from the cache.
     *
     * @param island island to remove
     */
    public synchronized void remove(Island island) {

        if (island == null) {
            return;
        }

        /*
        Supplying the expected Island value prevents
        accidentally removing a newer replacement
         */
        islandsById.remove(
                island.getIslandId(),
                island
        );

        islandsBySlot.remove(
                island.getSlotId(),
                island
        );

    }

    /**
     * Removes an island by its island ID
     *
     * @param islandId island ID
     * @return island if one existed
     */
    public synchronized Optional<Island> removeById(long islandId) {
        Island island = islandsById.get(islandId);

        if (island == null) {
            return Optional.empty();
        }

        remove(island);

        return Optional.of(island);
    }

    public Optional<Island> findById(long islandId) {
        return Optional.ofNullable(islandsById.get(islandId));
    }

    public Optional<Island> findBySlotId(long slotId) {
        return Optional.ofNullable(islandsBySlot.get(slotId));
    }

    public boolean existsById(long islandId) {
        return islandsById.containsKey(islandId);
    }

    public boolean existsBySlotId(long slotId) {
        return islandsBySlot.containsKey(slotId);
    }

    /**
     * Returns a snapshot of all cached islands.
     *
     * NOTE: MODIFYING THESE ISLANDS WILL NOT DO ANYTHING
     * ISLAND DATA MUST BE MODIFIED THROUGH THE HASHMAPS!!
     *
     * @return cached islands
     */
    public List<Island> getAll() {
        return List.copyOf(islandsById.values());
    }

    public int size() {
        return islandsById.size();
    }

    public boolean isEmpty() {
        return islandsById.isEmpty();
    }

    public synchronized void clear() {
        islandsById.clear();
        islandsBySlot.clear();
    }
}
