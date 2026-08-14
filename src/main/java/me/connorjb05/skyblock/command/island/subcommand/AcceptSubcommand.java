package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.Island;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

public class AcceptSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public AcceptSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "accept";
    }

    @Override
    public String description() {
        return "Accept an island invitation";
    }

    @Override
    public int minimumArgs() {
        return 1;
    }

    @Override
    public String usage() {
        return "/is accept <islandId>";
    }

    @Override
    public void execute(IslandCommandContext context) {
        Long islandId = IslandCommandUtil.parseIslandId(context.player(), context.args()[0]);

        if (islandId == null) {
            return;
        }

        plugin.getIslandService()
                .getMembershipService()
                .acceptInvite(context.player().getUniqueId(), islandId)
                .thenAccept(result -> IslandCommandUtil.sync(plugin, () -> {
                    if (!result.successful()) {
                        result.status().sendMessage(context.player());
                        return;
                    }

                    Island island = result.island();

                    result.status().sendMessage(
                            context.player(),
                            Placeholder.unparsed(
                                    "island_owner",
                                    IslandCommandUtil.leaderName(island)
                            )
                    );

                    plugin.getIslandService()
                            .getTeleportService()
                            .teleport(context.player().getUniqueId(), island);
                }))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();

                    IslandCommandUtil.sync(
                            plugin,
                            () -> IslandCommandStatus.JOIN_FAILURE.sendMessage(context.player())
                    );

                    return null;
                });
    }

    @Override
    public List<String> tabComplete(IslandCommandContext context) {
        if (context.args().length != 1) {
            return List.of();
        }

        long duration = plugin.getConfigService()
                .getIslandInviteSettings()
                .duration();

        List<String> invites = context.playerData()
                .getInvites()
                .stream()
                .filter(invite -> !invite.expired(duration))
                .map(invite -> Long.toString(invite.islandId()))
                .toList();

        return IslandCommandUtil.filter(invites, context.args()[0]);
    }
}
