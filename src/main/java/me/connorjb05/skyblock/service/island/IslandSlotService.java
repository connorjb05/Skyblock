package me.connorjb05.skyblock.service.island;

import java.util.*;

/***
 * Efficiently reuses island positions based on slot IDs
 */
public class IslandSlotService {

    private final Queue<Long> availableSlots = new PriorityQueue<>();
    private final Set<Long> availableSlotLookUp = new HashSet<>();

    private long nextSlotId;
    private boolean initialized;

    /**
     * Initializes the slot allocator using all currently occupied slot IDs
     * This should be called once during server startup before islands
     * are allowed to be created.
     *
     * @param occupiedSlots slots existing islands are occupying
     */
    public synchronized void populate(Collection<Long> occupiedSlots) {

        if (occupiedSlots == null) {
            throw new IllegalArgumentException(
                    "Occupied slots cannot be null."
            );
        }

        availableSlots.clear();
        availableSlotLookUp.clear();

        if (occupiedSlots.isEmpty()) {
            nextSlotId = 0;
            initialized = true;
            return;
        }

        Set<Long> occupiedSlotLookUp = new HashSet<>();

        for (Long slotId : occupiedSlots) {

            if (slotId == null) {
                throw new IllegalArgumentException(
                        "Occupied slot IDs cannot contain null."
                );
            }

            if (slotId < 0) {
                throw new IllegalArgumentException(
                        "Occupied slot ID cannot be negative:  " + slotId
                );
            }

            occupiedSlotLookUp.add(slotId);
        }

        long highestSlotId = occupiedSlotLookUp
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(-1);

        /*
        Every missing slot before the highest
        occupied slot can be reused
         */
        for (long slotId = 0; slotId < highestSlotId; slotId++) {
            if (!occupiedSlotLookUp.contains(slotId)) {
                availableSlots.offer(slotId);
                availableSlotLookUp.add(slotId);
            }
        }

        nextSlotId = highestSlotId + 1;
        initialized = true;
    }

    /**
     * Claims the lowest reusable slot.
     *
     * If no released slot exists, a new
     * slot ID is generated
     *
     * @return claimed slot ID
     */
    public synchronized long claimSlot() {
        ensureInitialized();

        Long reusableSlot = availableSlots.poll();

        if (reusableSlot != null) {
            availableSlotLookUp.remove(reusableSlot);
            return reusableSlot;
        }

        return nextSlotId++;
    }

    /**
     * Releases an occupied slot, so it may
     * be reused by another island
     *
     * @param slotId slot ID to release
     */
    public synchronized void releaseSlot(long slotId) {
        ensureInitialized();

        // nextSlotId itself has not been allocated yet.
        if (slotId < 0 || slotId >= nextSlotId) {
            throw new IllegalArgumentException(
                    "Cannot release invalid slot ID: " + slotId
            );
        }

        // Prevent the same slot from being released more than once.
        if (!availableSlotLookUp.add(slotId)) {
            return;
        }

        availableSlots.offer(slotId);
    }

    /**
     * Checks whether an existing slot has
     * been released and is available for reuse.
     *
     * @param slotId slot ID
     * @return whether the slot is reusable
     */
    public synchronized boolean isAvailable(long slotId) {
        ensureInitialized();
        return availableSlotLookUp.contains(slotId);
    }

    /**
     * Returns the next new slot ID.
     *
     * Note that claimSlot() may return a lower
     * reusable slot before this value.
     *
     * @return next new slot ID
     */
    public synchronized long getNextSlotId() {
        ensureInitialized();
        return nextSlotId;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "IslandSlotService has not been populated."
            );
        }
    }
}
