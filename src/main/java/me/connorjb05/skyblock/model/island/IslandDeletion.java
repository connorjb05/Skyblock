package me.connorjb05.skyblock.model.island;

public class IslandDeletion {

    private final long islandId;
    private final long slotId;

    private final int priority;

    private final long queuedAt;

    public IslandDeletion(
            long islandId,
            long slotId,
            int priority,
            long queuedAt
    ) {
        this.islandId = islandId;
        this.slotId = slotId;
        this.priority = priority;
        this.queuedAt = queuedAt;
    }

    public long getIslandId() {
        return islandId;
    }

    public long getSlotId() {
        return slotId;
    }

    public int getPriority() {
        return priority;
    }

    public long getQueuedAt() {
        return queuedAt;
    }
}
