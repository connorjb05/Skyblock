package me.connorjb05.skyblock.service.island.permission;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.IslandCache;
import me.connorjb05.skyblock.model.island.membership.IslandMember;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import me.connorjb05.skyblock.model.island.region.IslandRegionProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class IslandAccessService {

    private final IslandRegionProvider regionProvider;
    private final IslandCache islandCache;

    public IslandAccessService(
            IslandRegionProvider regionProvider,
            IslandCache islandCache
    ) {
        this.regionProvider = regionProvider;
        this.islandCache = islandCache;
    }

    public IslandRole getRole(Player player, Island island) {

        UUID playerId = player.getUniqueId();

        /*
        Island leader always has
        leader permissions.
         */
        if (island.getLeaderId().equals(playerId)) {
            return IslandRole.LEADER;
        }

        /*
        Primary lookup:
        island's runtime member cache.
         */
        IslandMember member = island.getMember(playerId)
                .orElse(null);

        if (member != null) {
            return member.role();
        }

        /*
        Fallback to PlayerData.

        This also protects against the island
        member cache temporarily being stale.
         */
        PlayerData playerData = Skyblock.get()
                .getPlayerDataService()
                .get(player);

        if (playerData == null
                || !playerData.hasIsland()
                || playerData.getIslandId() != island.getIslandId()
                || playerData.getIslandMember() == null) {

            return IslandRole.VISITOR;
        }

        return playerData.getIslandMember().role();
    }

    /*
    Checks an island permission
    without sending a message.
     */
    public boolean hasPermission(
            Player player,
            Island island,
            IslandPermission permission
    ) {

        IslandRole role = getRole(
                player,
                island
        );

        int requiredLevel = island.getPermissionData()
                .getPermissionLevel(permission);

        return role.getPermissionLevel() >= requiredLevel;
    }

    /**
     * Checks an island permission.
     *
     * A failure message is automatically
     * sent when permission is denied.
     *
     * @param player player to check
     * @param island island player is physically on
     * @param permission permission for action
     * @return
     */
    public boolean checkPermission(
            Player player,
            Island island,
            IslandPermission permission
    ) {

        if (hasPermission(
                player,
                island,
                permission
        )) {
            return true;
        }

        int requiredLevel = island.getPermissionData()
                .getPermissionLevel(permission);

        IslandRole requiredRole = IslandRole.fromPermissionLevel(requiredLevel);

        IslandPermissionStatus.NO_PERMISSION.sendMessage(
                player,
                Placeholder.unparsed(
                        "permission",
                        formatEnumName(permission.name())
                ),
                Placeholder.unparsed(
                        "required_role",
                        formatEnumName(requiredRole.name())
                )
        );

        return false;
    }

    public boolean hasPermission(
            Player player,
            Location location,
            IslandPermission permission
    ) {

        Island island = getIslandAt(location)
                        .orElse(null);

        /*
        No island owns this location.
         */
        if (island == null) {
            return true;
        }

        return hasPermission(
                player,
                island,
                permission
        );
    }

    public boolean checkPermission(
            Player player,
            Location location,
            IslandPermission permission
    ) {

        Island island = getIslandAt(location)
                .orElse(null);

        if (island == null) {
            return true;
        }

        return checkPermission(
                player,
                island,
                permission
        );
    }

    /**
     * Determines which ACTIVE island owns
     * a specific location.
     *
     * @param world
     * @param x
     * @param z
     * @return
     */
    public Optional<Island> getIslandAt(
            World world,
            int x,
            int z
    ) {

        OptionalLong slotId = regionProvider.getSlotIdAt(
                world.getName(),
                x,
                z
        );

        if (slotId.isEmpty()) {
            return Optional.empty();
        }

        return islandCache.findBySlotId(slotId.getAsLong());
    }

    public Optional<Island> getIslandAt(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        return getIslandAt(
                world,
                location.getBlockX(),
                location.getBlockZ()
        );
    }

    public Optional<Island> getIslandAt(Block block) {
        return getIslandAt(
                block.getWorld(),
                block.getX(),
                block.getZ()
        );
    }

    /*
    Checks whether the player can modify
    the supplied block.

    Non-island worlds are ignored by this
    protection system.
     */
    public boolean canModify(
            Player player,
            Block block
    ) {

        if (!regionProvider.isIslandWorld(block.getWorld().getName())) {
            return true;
        }

        Optional<Island> optionalIsland = getIslandAt(block);

        /*
        Inside the Skyblock world but outside
        an active island:

        Do not allow modification.
         */
        if (optionalIsland.isEmpty()) {
            return false;
        }

        Island island = optionalIsland.get();

        PlayerData playerData = Skyblock.get()
                        .getPlayerDataService()
                        .get(player);

        if (playerData == null || !playerData.hasIsland()) {
            return false;
        }

        /*
        For now, every island member can
        modify their own island.

        Later this can route through the
        IslandRole permission system.
         */
        return playerData.getIslandId() == island.getIslandId();
    }

    public boolean isIslandWorld(World world) {
        return regionProvider.isIslandWorld(world.getName());
    }

    private String formatEnumName(String s) {

        String[] parts = s.toLowerCase()
                        .split("_");

        StringBuilder builder = new StringBuilder();

        for (String part : parts) {

            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            builder.append(part.substring(1));
        }

        return builder.toString();
    }
}
