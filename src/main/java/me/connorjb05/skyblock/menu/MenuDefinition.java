package me.connorjb05.skyblock.menu;

import java.util.HashMap;
import java.util.Map;

public class MenuDefinition {

    private final String id;
    private final String title;
    private final int rows;
    private final MenuFillerDefinition filler;
    private final Map<String, MenuItemDefinition> items;

    /*
    Allows O(1) lookup from an inventory
    slot back to its configured item id
     */
    private final Map<Integer, String> slotLookup = new HashMap<>();

    public MenuDefinition(
            String id,
            String title,
            int rows,
            MenuFillerDefinition filler,
            Map<String, MenuItemDefinition> items
    ) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.filler = filler;
        this.items = Map.copyOf(items);

        populateSlotLookup();
    }

    private void populateSlotLookup() {

        int size = getSize();

        for (Map.Entry<String, MenuItemDefinition> entry : items.entrySet()) {

            MenuItemDefinition item = entry.getValue();

            if (!item.enabled()) {
                continue;
            }

            for (int slot : item.slots()) {

                if (slot < 0 || slot >= size) {
                    throw new IllegalStateException(
                            "Menu " + id + " contains invalid slot " + slot + "."
                    );
                }

                String previous = slotLookup.put(slot, entry.getKey());

                if (previous != null) {
                    throw new IllegalStateException(
                            "Menu " + id + " has multiple items in slot " + slot + "."
                    );
                }

            }

        }

    }

    public String getItemId(int slot) {
        return slotLookup.get(slot);
    }

    public MenuItemDefinition getItem(String id) {
        return items.get(id);
    }

    public int getSize() {
        return rows * 9;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public MenuFillerDefinition getFiller() {
        return filler;
    }

    public Map<String, MenuItemDefinition> getItems() {
        return items;
    }
}
