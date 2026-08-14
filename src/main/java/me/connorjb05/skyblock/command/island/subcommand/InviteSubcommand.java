package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.IslandInvite;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class InviteSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public InviteSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "invite";
    }

    @Override
    public String description() {
        return "Invite a player";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public IslandPermission requiredPermission() {
        return IslandPermission.INVITE_MEMBERS;
    }

    @Override
    public int minimumArgs() {
        return 1;
    }

    @Override
    public String usage() {
        return "/is invite <player>";
    }

    @Override
    public void execute(IslandCommandContext context) {
        Player player = context.player();
        Island island = context.island();

        Player target = Bukkit.getPlayerExact(context.args()[0]);

        if (target == null) {
            IslandCommandStatus.PLAYER_NOT_ONLINE.sendMessage(
                    player,
                    Placeholder.unparsed("player", context.args()[0])
            );
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            IslandCommandStatus.CANNOT_TARGET_SELF.sendMessage(player);
            return;
        }

        PlayerData targetData = plugin.getPlayerDataService().get(target);

        if (targetData == null) {
            IslandCommandStatus.TARGET_DATA_LOADING.sendMessage(
                    player,
                    Placeholder.unparsed("player", target.getName())
            );
            return;
        }

        if (targetData.hasIsland()) {
            IslandCommandStatus.TARGET_ALREADY_IN_ISLAND.sendMessage(
                    player,
                    Placeholder.unparsed("player", target.getName())
            );
            return;
        }

        long islandId = island.getIslandId();
        long duration = plugin.getConfigService().getIslandInviteSettings().duration();

        Optional<IslandInvite> existing = targetData.getInvite(islandId);

        if (existing.isPresent()) {
            if (existing.get().expired(duration)) {
                targetData.removeInvite(islandId);

            } else {
                IslandCommandStatus.INVITE_ALREADY_PENDING.sendMessage(
                        player,
                        Placeholder.unparsed("player", target.getName())
                );
                return;
            }
        }

        targetData.addInvite(new IslandInvite(islandId, System.currentTimeMillis()));

        IslandCommandStatus.INVITE_SENT.sendMessage(
                player,
                Placeholder.unparsed("player", target.getName())
        );

        IslandCommandStatus.INVITE_RECEIVED.sendMessage(
                target,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("island_id", Long.toString(islandId))
        );
    }

    @Override
    public List<String> tabComplete(IslandCommandContext context) {
        if (context.args().length != 1) {
            return List.of();
        }

        List<String> players = Bukkit.getOnlinePlayers()
                .stream()
                .filter(target -> !target.getUniqueId().equals(context.player().getUniqueId()))
                .filter(target -> {
                    PlayerData data = plugin.getPlayerDataService().get(target);
                    return data != null && !data.hasIsland();
                })
                .map(Player::getName)
                .toList();

        return IslandCommandUtil.filter(players, context.args()[0]);
    }
}
