package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class LeaveSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public LeaveSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public String description() {
        return "Leave your island";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public void execute(IslandCommandContext context) {
        plugin.getIslandService()
                .getMembershipService()
                .leave(context.player().getUniqueId())
                .thenAccept(result -> IslandCommandUtil.sync(plugin, () -> {
                    result.status().sendMessage(context.player());

                    if (!result.successful()) {
                        return;
                    }

                    World world = Bukkit.getWorld("world");

                    if (world != null) {
                        context.player().teleport(world.getSpawnLocation());
                    }
                }))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.LEAVE_FAILURE.sendMessage(context.player())
                    );

                    return null;
                });
    }
}
