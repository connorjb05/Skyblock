package me.connorjb05.skyblock.model.island;

import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.permission.IslandPermissionData;
import me.connorjb05.skyblock.model.island.settings.IslandSettingsData;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeData;

import java.util.*;

public class Island {

    private final long islandId;
    private final long slotId;

    private final Set<IslandMember> members = new HashSet<>();

    private final IslandUpgradeData upgradeData = new IslandUpgradeData();
    private final IslandPermissionData permissionData = new IslandPermissionData();
    private final IslandSettingsData settingsData = new IslandSettingsData();

    private UUID leaderId;

    public Island(
            long islandId,
            long slotId,
            UUID leaderId
    ) {
        this.islandId = islandId;
        this.slotId = slotId;
        this.leaderId = leaderId;
    }

    public Set<IslandMember> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Optional<IslandMember> getMember(UUID playerId) {
        return members.stream()
                .filter(member -> member.playerId().equals(playerId))
                .findFirst();
    }

    public boolean hasMember(UUID playerId) {
        return getMember(playerId).isPresent();
    }

    public void setMember(IslandMember member) {

        if (member.islandId() != islandId) {
            throw new IllegalArgumentException(
                    "Island member belongs to a different island."
            );
        }

        members.remove(member);

        members.add(member);
    }

    public void removeMember(UUID playerId) {
        members.removeIf(member -> member.playerId().equals(playerId));
    }

    public int getMemberCount() {
        return members.size();
    }

    public void clearMembers() {
        members.clear();
    }

    public long getIslandId() {
        return islandId;
    }

    public long getSlotId() {
        return slotId;
    }

    public IslandUpgradeData getUpgradeData() {
        return upgradeData;
    }

    public IslandPermissionData getPermissionData() {
        return permissionData;
    }

    public IslandSettingsData getSettingsData() {
        return settingsData;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }
}
