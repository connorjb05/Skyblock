package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.Island;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

public class DeclineSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public DeclineSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "decline";
    }

    @Override
    public String description() {
        return "Decline an island invitation";
    }

    @Override
    public int minimumArgs() {
        return 1;
    }

    @Override
    public String usage() {
        return "/is decline <islandId>";
    }

    @Override
    public void execute(IslandCommandContext context) {
        Long islandId = IslandCommandUtil.parseIslandId(context.player(), context.args()[0]);

        if (islandId == null) {
            return;
        }

        Island island = plugin.getIslandService()
                .getIsland(islandId)
                .orElse(null);

        var result = plugin.getIslandService()
                .getMembershipService()
                .declineInvite(context.player().getUniqueId(), islandId);

        if (!result.successful()) {
            result.status().sendMessage(context.player());
            return;
        }

        String owner = island == null
                ? "Unknown"
                : IslandCommandUtil.leaderName(island);

        result.status().sendMessage(
                context.player(),
                Placeholder.unparsed("island_owner", owner)
        );
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
