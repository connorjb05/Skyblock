package me.connorjb05.skyblock.menu.impl;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.menu.IslandMenuContext;
import me.connorjb05.skyblock.menu.MenuHolder;
import me.connorjb05.skyblock.menu.MenuService;
import me.connorjb05.skyblock.model.island.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public class IslandPanelMenu {

    private static final String MENU_ID = "island_panel_menu";

    private final Skyblock plugin;

    private final MenuService menuService;

    public IslandPanelMenu(
            Skyblock plugin,
            MenuService menuService
    ) {
        this.plugin = plugin;
        this.menuService = menuService;

        menuService.registerHandler(
                MENU_ID,
                this::onClick
        );
    }

    public void open(
            Player player,
            Island island
    ) {

        Inventory inventory = menuService.createInventory(
                MENU_ID,
                new IslandMenuContext(
                        island.getIslandId()
                )
        );

        menuService.renderItem(
                inventory,
                "home"
        );

        menuService.renderItem(
                inventory,
                "upgrades"
        );

        menuService.renderItem(
                inventory,
                "members"
        );

        menuService.renderItem(
                inventory,
                "permissions"
        );

        menuService.renderItem(
                inventory,
                "settings"
        );

        menuService.renderItem(
                inventory,
                "close"
        );

        player.openInventory(
                inventory
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

        Island island = plugin
                .getIslandService()
                .getIsland(
                        context.islandId()
                )
                .orElse(null);

        if (island == null) {
            player.closeInventory();
            return;
        }

        switch (itemId) {

            case "home" -> {

                player.closeInventory();

                plugin.getIslandService()
                        .getTeleportService()
                        .teleport(
                                player.getUniqueId(),
                                island
                        );
            }

            case "upgrades" ->
                    menuService
                            .getIslandUpgradeMenu()
                            .open(
                                    player,
                                    island
                            );

            case "members" -> {
                /*
                Member menu later.
                 */
            }

            case "permissions" ->
                    menuService
                            .getIslandPermissionMenu()
                            .open(
                                    player,
                                    island
                            );

            case "settings" ->
                    menuService
                            .getIslandSettingsMenu()
                            .open(
                                    player,
                                    island
                            );

            case "close" ->
                    player.closeInventory();
        }
    }
}