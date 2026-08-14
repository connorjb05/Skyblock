package me.connorjb05.skyblock.menu.impl;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.menu.IslandMenuContext;
import me.connorjb05.skyblock.menu.MenuHolder;
import me.connorjb05.skyblock.menu.MenuService;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import me.connorjb05.skyblock.model.island.upgrades.IslandUpgradeType;
import me.connorjb05.skyblock.model.island.upgrades.defintion.*;
import me.connorjb05.skyblock.service.config.settings.IslandUpgradeSettings;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

public class IslandUpgradeMenu {

    private static final String MENU_ID = "island_upgrades_menu";

    private final Skyblock plugin;
    private final MenuService menuService;

    private final Set<UUID> pendingUpgrades = new HashSet<>();

    public IslandUpgradeMenu(
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

        Inventory inventory = menuService.createInventory(
                MENU_ID,
                new IslandMenuContext(island.getIslandId())
        );

        for (IslandUpgradeType type : IslandUpgradeType.values()) {
            renderUpgrade(inventory, island, type);
        }

        menuService.renderItem(inventory, "back");

        player.openInventory(inventory);
    }

    private void renderUpgrade(
            Inventory inventory,
            Island island,
            IslandUpgradeType type
    ) {

        String itemId = type.name()
                .toLowerCase(Locale.ROOT);

        NavigableMap<Integer, ? extends IslandUpgradeLevel> definitions =
                getDefinitions(type);

        if (definitions.isEmpty()) {
            return;
        }

        int currentLevel = plugin.getIslandService()
                .getUpgradeService()
                .getLevel(island, type);

        IslandUpgradeLevel currentDefinition =
                definitions.get(currentLevel);

        if (currentDefinition == null) {
            throw new IllegalStateException(
                    "Missing " + type.name()
                            + " upgrade definition for level "
                            + currentLevel + "."
            );
        }

        Map.Entry<Integer, ? extends IslandUpgradeLevel> next =
                definitions.higherEntry(currentLevel);

        List<String> lore = expandNextInformation(
                menuService.getItemLore(MENU_ID, itemId),
                type,
                next
        );

        menuService.renderItem(
                inventory,
                itemId,
                lore,
                createResolvers(
                        currentLevel,
                        definitions.lastKey(),
                        currentDefinition
                )
        );
    }

    private TagResolver[] createResolvers(
            int currentLevel,
            int maxLevel,
            IslandUpgradeLevel definition
    ) {

        String currentValue = "";
        String currentChance = "";
        String speedMultiplier = "";
        String amountMultiplier = "";

        if (definition instanceof MultiplierUpgradeLevel multiplier) {
            currentValue = formatNumber(multiplier.multiplier());
            currentChance = formatNumber(multiplier.chance());
        }

        if (definition instanceof IslandSizeUpgradeLevel size) {
            currentValue = Integer.toString(size.size());
        }

        if (definition instanceof SpawnerUpgradeLevel spawner) {
            speedMultiplier = formatNumber(spawner.speedMultiplier());
            amountMultiplier = formatNumber(spawner.amountMultiplier());
            currentChance = formatNumber(spawner.chance());
        }

        return new TagResolver[]{
                Placeholder.unparsed(
                        "current_level",
                        Integer.toString(currentLevel)
                ),
                Placeholder.unparsed(
                        "max_level",
                        Integer.toString(maxLevel)
                ),
                Placeholder.unparsed(
                        "current_value",
                        currentValue
                ),
                Placeholder.unparsed(
                        "current_chance",
                        currentChance
                ),
                Placeholder.unparsed(
                        "speed_multiplier",
                        speedMultiplier
                ),
                Placeholder.unparsed(
                        "amount_multiplier",
                        amountMultiplier
                )
        };
    }

    private List<String> expandNextInformation(
            List<String> configuredLore,
            IslandUpgradeType type,
            Map.Entry<Integer, ? extends IslandUpgradeLevel> next
    ) {

        List<String> result = new ArrayList<>();

        for (String line : configuredLore) {

            if (!line.equalsIgnoreCase("<next_information>")) {
                result.add(line);
                continue;
            }

            result.addAll(createNextInformation(
                    type,
                    next
            ));
        }

        return result;
    }

    private List<String> createNextInformation(
            IslandUpgradeType type,
            Map.Entry<Integer, ? extends IslandUpgradeLevel> next
    ) {

        if (next == null) {
            return List.of(
                    "<green><bold>MAX LEVEL</bold>"
            );
        }

        IslandUpgradeLevel definition = next.getValue();

        List<String> information = new ArrayList<>();

        information.add(
                "<gray>Next Level: <white>" + next.getKey()
        );

        if (definition instanceof MultiplierUpgradeLevel multiplier) {

            information.add(
                    "<gray>Next Multiplier: <#FFD84D>x"
                            + formatNumber(multiplier.multiplier())
            );

            information.add(
                    "<gray>Next Chance: <#FFD84D>"
                            + formatNumber(multiplier.chance())
                            + "%"
            );
        }

        if (definition instanceof IslandSizeUpgradeLevel size) {
            information.add(
                    "<gray>Next Size: <#FFD84D>"
                            + size.size()
                            + "x"
                            + size.size()
            );
        }

        if (definition instanceof SpawnerUpgradeLevel spawner) {

            information.add(
                    "<gray>Next Speed: <#FFD84D>x"
                            + formatNumber(spawner.speedMultiplier())
            );

            information.add(
                    "<gray>Next Amount: <#FFD84D>x"
                            + formatNumber(spawner.amountMultiplier())
            );

            information.add(
                    "<gray>Next Chance: <#FFD84D>"
                            + formatNumber(spawner.chance())
                            + "%"
            );
        }

        if (definition instanceof OreGeneratorUpgradeLevel generator) {
            information.add(
                    "<gray>Generator Tier: <#FFD84D>"
                            + generator.level()
            );
        }

        information.add(
                "<gray>Cost: <#FFD84D>$"
                        + formatMoney(definition.cost())
        );

        information.add("");
        information.add(
                "<#FF9F1C><bold>CLICK TO UPGRADE</bold>"
        );

        return information;
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

        IslandUpgradeType type;

        try {
            type = IslandUpgradeType.valueOf(
                    itemId.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (!plugin.getIslandService()
                .getAccessService()
                .checkPermission(
                        player,
                        island,
                        IslandPermission.MANAGE_UPGRADES
                )) {
            return;
        }

        if (!pendingUpgrades.add(player.getUniqueId())) {
            return;
        }

        NavigableMap<Integer, ? extends IslandUpgradeLevel> definitions =
                getDefinitions(type);

        int currentLevel = plugin.getIslandService()
                .getUpgradeService()
                .getLevel(island, type);

        Map.Entry<Integer, ? extends IslandUpgradeLevel> next =
                definitions.higherEntry(currentLevel);

        if (next == null) {

            pendingUpgrades.remove(player.getUniqueId());

            player.sendMessage(plugin.getMessageService().parse(
                    "<green>This upgrade is already at its maximum level."
            ));

            open(player, island);
            return;
        }

        /*
        Cost is currently informational.

        Once the economy service is added,
        the balance check/withdrawal should
        happen before setLevel.
         */

        plugin.getIslandService()
                .getUpgradeService()
                .setLevel(
                        island,
                        type,
                        next.getKey()
                )
                .whenComplete((ignored, throwable) ->
                        Bukkit.getScheduler().runTask(plugin, () -> {

                            pendingUpgrades.remove(player.getUniqueId());

                            if (!player.isOnline()) {
                                return;
                            }

                            if (throwable != null) {

                                throwable.printStackTrace();

                                player.sendMessage(plugin.getMessageService().parse(
                                        "<red>Unable to upgrade "
                                                + formatEnum(type.name())
                                                + "."
                                ));

                                open(player, island);
                                return;
                            }

                            player.sendMessage(plugin.getMessageService().parse(
                                    "<green>Upgraded <yellow>"
                                            + formatEnum(type.name())
                                            + " <green>to level <yellow>"
                                            + next.getKey()
                                            + "<green>."
                            ));

                            open(player, island);
                        }));
    }

    private NavigableMap<Integer, ? extends IslandUpgradeLevel> getDefinitions(
            IslandUpgradeType type
    ) {

        IslandUpgradeSettings settings = plugin.getConfigService()
                .getIslandUpgradeSettings();

        return switch (type) {
            case ORE_GENERATOR -> settings.oreGenerator();
            case CROP_GROWTH -> settings.cropGrowth();
            case CROP_DROPS -> settings.cropDrops();
            case ISLAND_SIZE -> settings.islandSize();
            case SPAWNER -> settings.spawner();
            case MOB_DROPS -> settings.mobDrops();
            case MOB_EXPERIENCE -> settings.mobExperience();
        };
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String formatMoney(double value) {

        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);

        return format.format(value);
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
