package me.connorjb05.skyblock.menu.impl;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.menu.IslandMenuContext;
import me.connorjb05.skyblock.menu.MenuHolder;
import me.connorjb05.skyblock.menu.MenuService;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandPermissionsMenu {

    private static final String MENU_ID = "island_permissions_menu";

    /*
    Permission role cycle.

    Leader wraps back around
    to Visitor.
     */
    private static final List<IslandRole> ROLE_CYCLE = List.of(
            IslandRole.VISITOR,
            IslandRole.MEMBER,
            IslandRole.MODERATOR,
            IslandRole.CO_LEADER,
            IslandRole.LEADER
    );

    private final Skyblock plugin;
    private final MenuService menuService;

    private final Set<UUID> pendingChanges = new HashSet<>();

    public IslandPermissionsMenu(
            Skyblock plugin,
            MenuService menuService
    ) {
        this.plugin = plugin;
        this.menuService = menuService;

        menuService.registerHandler(MENU_ID, this::onClick);
    }

    public void open(
            Player player,
            Island island
    ) {

        if (!plugin.getIslandService()
                .getAccessService()
                .checkPermission(
                        player,
                        island,
                        IslandPermission.MANAGE_PERMISSIONS
                )) {
            return;
        }

        Inventory inventory = menuService.createInventory(
                MENU_ID,
                new IslandMenuContext(island.getIslandId())
        );

        for (IslandPermission permission : IslandPermission.values()) {
            renderPermission(inventory, island, permission);
        }

        menuService.renderItem(inventory, "reset_all");
        menuService.renderItem(inventory, "back");

        player.openInventory(inventory);
    }

    private void renderPermission(
            Inventory inventory,
            Island island,
            IslandPermission permission
    ) {

        int permissionLevel = island.getPermissionData()
                .getPermissionLevel(permission);

        IslandRole currentRole = getRole(permissionLevel);
        IslandRole nextRole = getNextRole(currentRole);

        String itemId = permission.name()
                .toLowerCase(Locale.ROOT);

        menuService.renderItem(
                inventory,
                itemId,

                Placeholder.unparsed(
                        "current_role",
                        formatEnum(currentRole.name())
                ),

                Placeholder.unparsed(
                        "next_role",
                        formatEnum(nextRole.name())
                ),

                Placeholder.unparsed(
                        "default_role",
                        formatEnum(
                                permission.getDefaultRole().name()
                        )
                ),

                Placeholder.unparsed(
                        "override",
                        island.getPermissionData()
                                .hasOverride(permission)
                                ? "Customized"
                                : "Default"
                )
        );
    }

    private void onClick(
            Player player,
            MenuHolder holder,
            String itemId,
            ClickType clickType
    ) {

        if (!(holder.getContext() instanceof IslandMenuContext context)) {
            return;
        }

        Island island = plugin.getIslandService()
                .getIsland(context.islandId())
                .orElse(null);

        if (island == null) {
            player.closeInventory();
            return;
        }

        if (itemId.equalsIgnoreCase("back")) {
            menuService.getIslandPanelMenu()
                    .open(player, island);
            return;
        }

        if (!plugin.getIslandService()
                .getAccessService()
                .checkPermission(
                        player,
                        island,
                        IslandPermission.MANAGE_PERMISSIONS
                )) {
            player.closeInventory();
            return;
        }

        if (!pendingChanges.add(player.getUniqueId())) {
            return;
        }

        if (itemId.equalsIgnoreCase("reset_all")) {

            CompletableFuture<?> future = plugin.getIslandService()
                    .getPermissionService()
                    .resetAll(player, island);

            finishChange(
                    player,
                    island,
                    future
            );

            return;
        }

        IslandPermission permission;

        try {
            permission = IslandPermission.valueOf(
                    itemId.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ignored) {
            pendingChanges.remove(player.getUniqueId());
            return;
        }

        int currentLevel = island.getPermissionData()
                .getPermissionLevel(permission);

        IslandRole currentRole = getRole(currentLevel);
        IslandRole nextRole = getNextRole(currentRole);

        CompletableFuture<?> future = plugin.getIslandService()
                .getPermissionService()
                .setPermissionLevel(
                        player,
                        island,
                        permission,
                        nextRole
                );

        finishChange(
                player,
                island,
                future
        );
    }

    private void finishChange(
            Player player,
            Island island,
            CompletableFuture<?> future
    ) {

        future.whenComplete((ignored, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> {

                    pendingChanges.remove(player.getUniqueId());

                    if (!player.isOnline()) {
                        return;
                    }

                    if (throwable != null) {
                        throwable.printStackTrace();
                    }

                    /*
                    Changing MANAGE_PERMISSIONS itself
                    can remove the player's access.

                    In that case close the menu rather
                    than leave stale clickable items.
                     */
                    if (!plugin.getIslandService()
                            .getAccessService()
                            .hasPermission(
                                    player,
                                    island,
                                    IslandPermission.MANAGE_PERMISSIONS
                            )) {

                        player.closeInventory();
                        return;
                    }

                    open(player, island);
                }));
    }

    private IslandRole getRole(int permissionLevel) {

        for (IslandRole role : ROLE_CYCLE) {
            if (role.getPermissionLevel() == permissionLevel) {
                return role;
            }
        }

        return IslandRole.VISITOR;
    }

    private IslandRole getNextRole(IslandRole currentRole) {

        int index = ROLE_CYCLE.indexOf(currentRole);

        if (index < 0) {
            return IslandRole.VISITOR;
        }

        int nextIndex = (index + 1) % ROLE_CYCLE.size();

        return ROLE_CYCLE.get(nextIndex);
    }

    private String formatEnum(String value) {

        String[] parts = value.toLowerCase(Locale.ROOT)
                .split("_");

        StringBuilder builder = new StringBuilder();

        for (String part : parts) {

            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(
                    Character.toUpperCase(part.charAt(0))
            );

            builder.append(
                    part.substring(1)
            );
        }

        return builder.toString();
    }
}
