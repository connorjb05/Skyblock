package me.connorjb05.skyblock.model.island;

/*
Represents a pending island invitation
 */
public record IslandInvite(
        long islandId,
        long timestamp
) {

    /**
     * Checks whether this invitation has expired.
     *
     * @param duration invite duration in milliseconds
     * @return whether the invite has expired
     */
    public boolean expired(long duration) {
        return System.currentTimeMillis() - timestamp >= duration;
    }

}
