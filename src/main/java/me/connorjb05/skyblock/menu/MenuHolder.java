package me.connorjb05.skyblock.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {

    private final MenuDefinition definition;

    private final MenuContext context;

    private Inventory inventory;

    public MenuHolder(
            MenuDefinition definition,
            MenuContext context
    ) {
        this.definition = definition;
        this.context = context;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuDefinition getDefinition() {
        return definition;
    }

    public MenuContext getContext() {
        return context;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
