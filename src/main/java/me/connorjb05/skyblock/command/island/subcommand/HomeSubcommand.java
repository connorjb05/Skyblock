package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.Island;
import org.bukkit.entity.Player;

import java.util.Set;

public class HomeSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public HomeSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "home";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("go");
    }

    @Override
    public String description() {
        return "Teleport to your island";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public void execute(IslandCommandContext context) {
        Player player = context.player();
        Island island = context.island();

        plugin.getIslandService()
                .getTeleportService()
                .teleport(player.getUniqueId(), island)
                .thenAccept(success -> {
                    if (success) {
                        return;
                    }

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.TELEPORT_FAILURE.sendMessage(player)
                    );
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.TELEPORT_FAILURE.sendMessage(player)
                    );

                    return null;
                });
    }
}
