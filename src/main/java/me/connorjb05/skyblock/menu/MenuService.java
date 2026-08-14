package me.connorjb05.skyblock.menu;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.menu.impl.IslandPanelMenu;
import me.connorjb05.skyblock.menu.impl.IslandPermissionsMenu;
import me.connorjb05.skyblock.menu.impl.IslandSettingsMenu;
import me.connorjb05.skyblock.menu.impl.IslandUpgradeMenu;
import me.connorjb05.skyblock.service.Service;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class MenuService implements Service, Listener {

    private static final List<String> DEFAULT_MENUS =
            List.of(
                    "island_panel_menu.yml",
                    "island_upgrades_menu.yml",
                    "island_permissions_menu.yml",
                    "island_settings_menu.yml"
            );

    private final Map<String, MenuDefinition> menus = new HashMap<>();

    private final Map<String, MenuClickEvent> handlers = new HashMap<>();

    private Skyblock plugin;

    /*
    Put menu objects here:
     */
    private IslandPanelMenu islandPanelMenu;
    private IslandUpgradeMenu islandUpgradeMenu;
    private IslandPermissionsMenu islandPermissionsMenu;
    private IslandSettingsMenu islandSettingsMenu;

    @Override
    public void onEnable(Skyblock plugin) {
        this.plugin = plugin;

        saveDefaultMenus();

        reload();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        islandPanelMenu = new IslandPanelMenu(
                plugin,
                this
        );

        islandUpgradeMenu = new IslandUpgradeMenu(
                plugin,
                this
        );

        islandPermissionsMenu = new IslandPermissionsMenu(
                plugin,
                this
        );

        islandSettingsMenu = new IslandSettingsMenu(
                plugin,
                this
        );
    }

    @Override
    public void onDisable(Skyblock plugin) {
        HandlerList.unregisterAll(this);

        menus.clear();
        handlers.clear();

        this.plugin = null;
    }

    public IslandPanelMenu getIslandPanelMenu() {
        return islandPanelMenu;
    }

    public IslandUpgradeMenu getIslandUpgradeMenu() {
        return islandUpgradeMenu;
    }

    public IslandPermissionsMenu getIslandPermissionMenu() {
        return islandPermissionsMenu;
    }

    public IslandSettingsMenu getIslandSettingsMenu() {
        return islandSettingsMenu;
    }

    /*
    Copies default menu configurations
    from the plugin jar if they do not
    already exist.
     */
    private void saveDefaultMenus() {

        File folder = new File(plugin.getDataFolder(), "menus");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Unable to create menus directory."
            );
        }

        for (String fileName : DEFAULT_MENUS) {

            File file = new File(
                    folder,
                    fileName
            );

            if (file.exists()) {
                continue;
            }

            plugin.saveResource(
                    "menus/" + fileName,
                    false
            );
        }
    }

    /*
    Reloads every menu configuration
     */
    private void reload() {

        File folder = new File(plugin.getDataFolder(), "menus");

        File[] files = folder.listFiles(
                file -> file.isFile()
                        && file.getName().toLowerCase(Locale.ROOT)
                        .endsWith(".yml")
        );

        if (files == null) {
            return;
        }

        Map<String, MenuDefinition> loaded = new HashMap<>();

        for (File file : files) {

            MenuDefinition definition = loadMenu(file);

            loaded.put(
                    definition.getId(),
                    definition
            );
        }

        menus.clear();
        menus.putAll(loaded);
    }

    private MenuDefinition loadMenu(File file) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String fileName = file.getName();

        String id = fileName.substring(0, fileName.length() - 4);

        String title = config.getString(
                "title",
                "<white>Menu"
        );

        int rows = config.getInt(
                "rows",
                3
        );

        if (rows < 1 || rows > 6) {
            throw new IllegalStateException(
                    "Menu " + id + " must contain between 1 and 6 rows."
            );
        }

        MenuFillerDefinition filler = loadFiller(id, config);

        Map<String, MenuItemDefinition> items = loadItems(id, config);

        return new MenuDefinition(
                id,
                title,
                rows,
                filler,
                items
        );
    }

    private MenuFillerDefinition loadFiller(
            String menuId,
            YamlConfiguration config
    ) {

        boolean enabled = config.getBoolean(
                "filler.enabled",
                false
        );

        String materialName = config.getString(
                "filler.material",
                "GRAY_STAINED_GLASS_PANE"
        );

        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            throw new IllegalStateException(
                    "Invalid filler material " + materialName +
                            " in menu " + menuId + "."
            );
        }

        String name = config.getString(
                "filler.name",
                 " "
        );

        return new MenuFillerDefinition(
                enabled,
                material,
                name
        );
    }

    private Map<String, MenuItemDefinition> loadItems(
            String menuId,
            YamlConfiguration config
    ) {

        Map<String, MenuItemDefinition> items = new HashMap<>();

        ConfigurationSection section = config.getConfigurationSection("items");

        if (section == null) {
            return items;
        }

        for (String itemId : section.getKeys(false)) {

            String path = "items." + itemId + ".";

            boolean enabled = config.getBoolean(
                    path + "enabled",
                    true
            );

            String materialName = config.getString(
                    path + "material",
                    "STONE"
            );

            Material material = Material.matchMaterial(materialName);

            if (material == null) {
                throw new IllegalStateException(
                        "Invalid material "
                                + materialName
                                + " for "
                                + menuId
                                + "."
                                + itemId
                                + "."
                );
            }

            int amount = config.getInt(
                    path + "amount",
                    1
            );

            if (amount < 1 || amount > 64) {
                amount = 1;
            }

            List<Integer> slots = loadSlots(
                    config,
                    path
            );

            String name = config.getString(
                    path + "name",
                    ""
            );

            List<String> lore = config.getStringList(
                    path + "lore"
            );

            boolean glow = config.getBoolean(
                    path + "glow",
                    false
            );

            items.put(itemId, new MenuItemDefinition(
                    enabled,
                    List.copyOf(slots),
                    material,
                    amount,
                    name,
                    List.copyOf(lore),
                    glow
            ));
        }

        return items;
    }

    private List<Integer> loadSlots(
            YamlConfiguration config,
            String path
    ) {

        if (config.contains(path + "slots")) {
            return config.getIntegerList(path + "slots");
        }

        if (config.contains(path + "slot")) {
            return List.of(config.getInt(path + "slot"));
        }

        return List.of();
    }

    /*
    Creates the inventory and its filler

    Menu-specific classes are responsible for
    rendering their configured items.
     */
    public Inventory createInventory(
            String menuId,
            MenuContext context,
            TagResolver... titleResolvers
    ) {

        MenuDefinition definition = getMenu(menuId);
        MenuHolder holder = new MenuHolder(definition, context);

        Component title = plugin.getMessageService().parse(
                definition.getTitle(),
                titleResolvers
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                definition.getSize(),
                title
        );

        holder.setInventory(inventory);

        applyFiller(inventory, definition);

        return inventory;
    }

    /*
Renders one configured menu item.
 */
    public void renderItem(
            Inventory inventory,
            String itemId,
            TagResolver... resolvers
    ) {
        renderItem(inventory, itemId, null, resolvers);
    }

    /*
    Renders one configured menu item
    with custom lore.

    Useful for menu-specific dynamic
    multi-line information.
     */
    public void renderItem(
            Inventory inventory,
            String itemId,
            List<String> loreOverride,
            TagResolver... resolvers
    ) {

        if (!(inventory.getHolder() instanceof MenuHolder holder)) {
            throw new IllegalArgumentException(
                    "Inventory is not a configured menu."
            );
        }

        MenuItemDefinition definition = holder.getDefinition()
                .getItem(itemId);

        if (definition == null || !definition.enabled()) {
            return;
        }

        List<String> lore = loreOverride != null
                ? loreOverride
                : definition.lore();

        ItemStack item = createItem(
                definition,
                lore,
                resolvers
        );

        for (int slot : definition.slots()) {
            inventory.setItem(slot, item.clone());
        }
    }

    public List<String> getItemLore(
            String menuId,
            String itemId
    ) {

        MenuItemDefinition definition = getMenu(menuId)
                .getItem(itemId);

        if (definition == null) {
            return List.of();
        }

        return definition.lore();
    }

    private ItemStack createItem(
            MenuItemDefinition definition,
            List<String> loreLines,
            TagResolver... resolvers
    ) {

        ItemStack item = new ItemStack(
                definition.material(),
                definition.amount()
        );

        ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.getMessageService().parse(
                definition.name(),
                resolvers
        ));

        List<Component> lore = new ArrayList<>();

        for (String line : loreLines) {
            lore.add(plugin.getMessageService().parse(
                    line,
                    resolvers
            ));
        }

        meta.lore(lore);

        if (definition.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createItem(
            MenuItemDefinition definition,
            TagResolver... resolvers
    ) {

        ItemStack item = new ItemStack(
                definition.material(),
                definition.amount()
        );

        ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.getMessageService().parse(
                definition.name(),
                resolvers
        ));

        List<Component> lore = new ArrayList<>();

        for (String line : definition.lore()) {
            lore.add(plugin.getMessageService().parse(
                    line,
                    resolvers
            ));
        }

        meta.lore(lore);

        if (definition.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);

        return item;
    }

    private void applyFiller(
            Inventory inventory,
            MenuDefinition definition
    ) {

        MenuFillerDefinition filler = definition.getFiller();

        if (filler == null || !filler.enabled()) {
            return;
        }

        ItemStack item = new ItemStack(filler.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getMessageService().parse(
                filler.name()
        ));
        item.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    public void registerHandler(
            String menuId,
            MenuClickEvent event
    ) {
        handlers.put(menuId, event);
    }

    public MenuDefinition getMenu(String menuId) {
        MenuDefinition definition = menus.get(menuId);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown menu: " + menuId
            );
        }

        return definition;
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        Inventory inventory = e.getView().getTopInventory();

        if (!(inventory.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        /*
        Plugin menus are read only.
         */
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = e.getRawSlot();

        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        String itemId = holder.getDefinition()
                .getItemId(slot);

        if (itemId == null) {
            return;
        }

        MenuClickEvent event = handlers.get(holder.getDefinition().getId());

        if (event == null) {
            return;
        }

        event.onClick(
                player,
                holder,
                itemId,
                e.getClick()
        );
    }

    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }
}
