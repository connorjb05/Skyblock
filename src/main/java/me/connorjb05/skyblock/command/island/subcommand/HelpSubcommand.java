package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandSubcommand;

import java.util.List;

public class HelpSubcommand implements IslandSubcommand {

    private final Skyblock plugin;
    private final List<IslandSubcommand> subcommands;

    public HelpSubcommand(
            Skyblock plugin,
            List<IslandSubcommand> subcommands
    ) {
        this.plugin = plugin;
        this.subcommands = subcommands;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "View island commands";
    }

    @Override
    public void execute(IslandCommandContext context) {
        plugin.getMessageService().send(
                context.player(),
                "<#FF9F1C><bold>Skyblock Commands</bold>"
        );

        for (IslandSubcommand subcommand : subcommands) {
            if (subcommand == this) {
                continue;
            }

            plugin.getMessageService().send(
                    context.player(),
                    "<#FFD84D>"
                            + subcommand.usage()
                            + " <white>- "
                            + subcommand.description()
            );
        }
    }
}
