package me.connorjb05.skyblock.model;

import fr.mrmicky.fastboard.adventure.FastBoard;
import me.connorjb05.skyblock.model.island.IslandInvite;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final Player player;
    private final UUID uuid;
    private final FastBoard scoreboard;

    private final PlayerBorderData borderData = new PlayerBorderData();

    private final Map<Long, IslandInvite> islandInvites = new ConcurrentHashMap<>();

    private boolean loaded = false;
    private IslandMember islandMember = null;
    private long islandId = -1L;

    public PlayerData(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.scoreboard = new FastBoard(player);
    }

    public void close() {
        scoreboard.delete();
        islandInvites.clear();
    }

    public PlayerBorderData getBorderData() {
        return borderData;
    }

    public FastBoard getScoreboard() {
        return scoreboard;
    }

    public void addInvite(IslandInvite invite) {
        islandInvites.put(invite.islandId(), invite);
    }

    public Optional<IslandInvite> getInvite(long islandId) {
        return Optional.ofNullable(islandInvites.get(islandId));
    }

    public boolean hasInvite(long islandId) {
        return islandInvites.containsKey(islandId);
    }

    public void removeInvite(long islandId) {
        islandInvites.remove(islandId);
    }

    public void clearInvites() {
        islandInvites.clear();
    }

    public Collection<IslandInvite> getInvites() {
        return List.copyOf(islandInvites.values());
    }

    public void removeExpiredInvites(long duration) {
        islandInvites.values()
                .removeIf(invite -> invite.expired(duration));
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public IslandMember getIslandMember() {
        return islandMember;
    }

    public void setIslandMember(IslandMember islandMember) {
        this.islandMember = islandMember;
    }

    public long getIslandId() {
        return islandId;
    }

    public void setIslandId(long islandId) {
        this.islandId = islandId;
    }

    public boolean hasIsland() {
        return islandId > -1L;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getUuid() {
        return uuid;
    }
}
