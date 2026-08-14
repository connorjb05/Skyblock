package me.connorjb05.skyblock.menu.impl;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.menu.IslandMenuContext;
import me.connorjb05.skyblock.menu.MenuHolder;
import me.connorjb05.skyblock.menu.MenuService;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import me.connorjb05.skyblock.model.island.settings.IslandSetting;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandSettingsMenu {

    private static final String MENU_ID = "island_settings_menu";

    private final Skyblock plugin;

    private final MenuService menuService;

    /*
    Prevents rapid clicking from creating
    multiple database updates for the
    same player at once.
     */
    private final Set<UUID> pendingChanges = new HashSet<>();

    public IslandSettingsMenu(
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

        if (!plugin.getIslandService()
                .getAccessService()
                .checkPermission(
                        player,
                        island,
                        IslandPermission.MANAGE_SETTINGS
                )) {
            return;
        }

        Inventory inventory = menuService.createInventory(
                MENU_ID,
                new IslandMenuContext(
                        island.getIslandId()
                )
        );

        for (IslandSetting setting : IslandSetting.values()) {
            renderSetting(
                    inventory,
                    island,
                    setting
            );
        }

        menuService.renderItem(
                inventory,
                "reset_all"
        );

        menuService.renderItem(
                inventory,
                "back"
        );

        player.openInventory(
                inventory
        );
    }

    private void renderSetting(
            Inventory inventory,
            Island island,
            IslandSetting setting
    ) {

        boolean value = island
                .getSettingsData()
                .getValue(setting);

        boolean defaultValue =
                setting.getDefaultValue();

        String itemId = setting.name()
                .toLowerCase(Locale.ROOT);

        menuService.renderItem(
                inventory,
                itemId,

                Placeholder.unparsed(
                        "status",
                        formatBoolean(value)
                ),

                Placeholder.parsed(
                        "status_color",
                        value
                                ? "<green>"
                                : "<red>"
                ),

                Placeholder.unparsed(
                        "default_status",
                        formatBoolean(defaultValue)
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

        if (itemId.equalsIgnoreCase("back")) {

            menuService.getIslandPanelMenu()
                    .open(
                            player,
                            island
                    );

            return;
        }

        if (!plugin.getIslandService()
                .getAccessService()
                .checkPermission(
                        player,
                        island,
                        IslandPermission.MANAGE_SETTINGS
                )) {

            player.closeInventory();
            return;
        }

        if (!pendingChanges.add(
                player.getUniqueId()
        )) {
            return;
        }

        if (itemId.equalsIgnoreCase("reset_all")) {

            CompletableFuture<?> future =
                    plugin.getIslandService()
                            .getSettingsService()
                            .resetAll(island);

            finishChange(
                    player,
                    island,
                    future
            );

            return;
        }

        IslandSetting setting;

        try {

            setting = IslandSetting.valueOf(
                    itemId.toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException ignored) {

            pendingChanges.remove(
                    player.getUniqueId()
            );

            return;
        }

        CompletableFuture<?> future =
                plugin.getIslandService()
                        .getSettingsService()
                        .toggle(
                                island,
                                setting
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

        future.whenComplete(
                (ignored, throwable) ->
                        Bukkit.getScheduler()
                                .runTask(
                                        plugin,
                                        () -> {

                                            pendingChanges.remove(
                                                    player.getUniqueId()
                                            );

                                            if (!player.isOnline()) {
                                                return;
                                            }

                                            if (throwable != null) {

                                                throwable.printStackTrace();

                                                player.sendMessage(
                                                        plugin.getMessageService()
                                                                .parse(
                                                                        "<red>Unable to update the island setting."
                                                                )
                                                );

                                                return;
                                            }

                                            if (!plugin.getIslandService()
                                                    .getAccessService()
                                                    .hasPermission(
                                                            player,
                                                            island,
                                                            IslandPermission.MANAGE_SETTINGS
                                                    )) {

                                                player.closeInventory();
                                                return;
                                            }

                                            open(
                                                    player,
                                                    island
                                            );
                                        }
                                )
        );
    }

    private String formatBoolean(boolean value) {
        return value
                ? "Enabled"
                : "Disabled";
    }
}
