package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandSubcommand;

public class MenuSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public MenuSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "menu";
    }

    @Override
    public String description() {
        return "Open the island menu";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public void execute(IslandCommandContext context) {
        plugin.getMenuService()
                .getIslandPanelMenu()
                .open(context.player(), context.island());
    }
}
