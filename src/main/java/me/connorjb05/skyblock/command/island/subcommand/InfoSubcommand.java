package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.Island;

public class InfoSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public InfoSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String description() {
        return "View island information";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public void execute(IslandCommandContext context) {
        Island island = context.island();

        plugin.getMessageService().send(
                context.player(),
                "<#FF9F1C><bold>Island Information</bold>"
        );

        plugin.getMessageService().send(
                context.player(),
                "<#FFD84D>Island ID: <white>" + island.getIslandId()
        );

        plugin.getMessageService().send(
                context.player(),
                "<#FFD84D>Slot ID: <white>" + island.getSlotId()
        );

        plugin.getMessageService().send(
                context.player(),
                "<#FFD84D>Leader: <white>" + IslandCommandUtil.leaderName(island)
        );

        plugin.getMessageService().send(
                context.player(),
                "<#FFD84D>Role: <white>"
                        + IslandCommandUtil.prettyEnum(
                                context.playerData().getIslandMember().role().name()
                        )
        );
    }
}
