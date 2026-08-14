package me.connorjb05.skyblock.model.island.membership;

import java.util.Objects;
import java.util.UUID;

/*
Represents a player's persistent
membership within an island.
 */
public record IslandMember(
        UUID playerId,
        long islandId,
        IslandRole role,
        long timeJoined
) {

    public IslandMember {

        Objects.requireNonNull(
                playerId,
                "playerId"
        );

        Objects.requireNonNull(
                role,
                "role"
        );

    }

    /*
    An island membership is uniquely
    identified by the island and player.

    Role is intentionally not included
    so a role change cannot create a
    duplicate member in a Set.
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof IslandMember other)) {
            return false;
        }

        return islandId == other.islandId
                && playerId.equals(
                other.playerId
        );

    }

    @Override
    public int hashCode() {
        return Objects.hash(
                islandId,
                playerId
        );
    }

}
