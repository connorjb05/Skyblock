package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import org.bukkit.entity.Player;

public class CreateSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public CreateSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String description() {
        return "Create an island";
    }

    @Override
    public void execute(IslandCommandContext context) {
        Player player = context.player();

        plugin.getIslandService()
                .getCreationService()
                .create(player.getUniqueId())
                .thenAccept(result -> IslandCommandUtil.sync(
                        plugin,
                        () -> result.status().sendMessage(player)
                ))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.CREATE_FAILURE.sendMessage(player)
                    );

                    return null;
                });
    }
}
