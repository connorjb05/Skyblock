package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;

public class DeleteSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public DeleteSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("disband");
    }

    @Override
    public String description() {
        return "Delete your island";
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
                .getDeletionService()
                .delete(player.getUniqueId(), island)
                .thenAccept(result -> IslandCommandUtil.sync(plugin, () -> {
                    result.status().sendMessage(player);

                    if (!result.successful()) {
                        return;
                    }

                    World world = Bukkit.getWorld("world");

                    if (world != null) {
                        player.teleport(world.getSpawnLocation());
                    }
                }))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.DELETE_FAILURE.sendMessage(player)
                    );

                    return null;
                });
    }
}
