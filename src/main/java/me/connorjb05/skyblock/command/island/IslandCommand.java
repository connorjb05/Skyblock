package me.connorjb05.skyblock.command.island;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.subcommand.*;
import me.connorjb05.skyblock.command.island.subcommand.CreateSubcommand;
import me.connorjb05.skyblock.command.island.subcommand.MenuSubcommand;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IslandCommand implements CommandExecutor, TabCompleter {

    private final Skyblock plugin;

    private final Map<String, IslandSubcommand> lookup = new HashMap<>();
    private final List<IslandSubcommand> subcommands = new ArrayList<>();

    private final IslandSubcommand menuSubcommand;

    public IslandCommand(Skyblock plugin) {
        this.plugin = plugin;

        menuSubcommand = new MenuSubcommand(plugin);

        register(menuSubcommand);
        register(new CreateSubcommand(plugin));
        register(new HomeSubcommand(plugin));
        register(new DeleteSubcommand(plugin));
        register(new InfoSubcommand(plugin));
        register(new InviteSubcommand(plugin));
        register(new AcceptSubcommand(plugin));
        register(new DeclineSubcommand(plugin));
        register(new LeaveSubcommand(plugin));
        register(new PermissionSubcommand(plugin));

        /*
        Help is registered last so it can
        use the completed command list.
         */
        register(new HelpSubcommand(plugin, subcommands));
    }

    private void register(IslandSubcommand subcommand) {
        subcommands.add(subcommand);
        lookup.put(subcommand.name().toLowerCase(Locale.ROOT), subcommand);

        for (String alias : subcommand.aliases()) {
            lookup.put(alias.toLowerCase(Locale.ROOT), subcommand);
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "<red>This command can only be used by players.");
            return true;
        }

        PlayerData playerData = plugin.getPlayerDataService().get(player);

        if (playerData == null) {
            IslandCommandStatus.PLAYER_DATA_LOADING.sendMessage(player);
            return true;
        }

        if (args.length == 0) {
            return dispatch(player, playerData, menuSubcommand, new String[0]);
        }

        IslandSubcommand subcommand = lookup.get(args[0].toLowerCase(Locale.ROOT));

        if (subcommand == null) {
            return dispatch(player, playerData, menuSubcommand, new String[0]);
        }

        return dispatch(
                player,
                playerData,
                subcommand,
                Arrays.copyOfRange(args, 1, args.length)
        );
    }

    private boolean dispatch(
            Player player,
            PlayerData playerData,
            IslandSubcommand subcommand,
            String[] args
    ) {

        Island island = null;

        if (subcommand.requiresIsland()) {
            if (!playerData.hasIsland()) {
                IslandCommandStatus.NOT_IN_ISLAND.sendMessage(player);
                return true;
            }

            island = plugin.getIslandService()
                    .getIsland(playerData.getIslandId())
                    .orElse(null);

            if (island == null) {
                IslandCommandStatus.ISLAND_NOT_FOUND.sendMessage(player);
                return true;
            }
        }

        IslandPermission permission = subcommand.requiredPermission();

        if (permission != null && !plugin.getIslandService()
                .getAccessService()
                .checkPermission(player, island, permission)) {

            return true;
        }

        if (args.length < subcommand.minimumArgs()) {
            IslandCommandUtil.usage(player, subcommand.usage());
            return true;
        }

        subcommand.execute(
                new IslandCommandContext(
                        plugin,
                        player,
                        playerData,
                        island,
                        args
                )
        );

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {
            return List.of();
        }

        PlayerData playerData = plugin.getPlayerDataService().get(player);

        if (playerData == null) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> available = new ArrayList<>();

            for (IslandSubcommand subcommand : subcommands) {
                if (!canUseSilently(player, playerData, subcommand)) {
                    continue;
                }

                available.add(subcommand.name());
                available.addAll(subcommand.aliases());
            }

            return IslandCommandUtil.filter(available, args[0]);
        }

        IslandSubcommand subcommand = lookup.get(args[0].toLowerCase(Locale.ROOT));

        if (subcommand == null || !canUseSilently(player, playerData, subcommand)) {
            return List.of();
        }

        Island island = null;

        if (subcommand.requiresIsland()) {
            island = plugin.getIslandService()
                    .getIsland(playerData.getIslandId())
                    .orElse(null);

            if (island == null) {
                return List.of();
            }
        }

        return subcommand.tabComplete(
                new IslandCommandContext(
                        plugin,
                        player,
                        playerData,
                        island,
                        Arrays.copyOfRange(args, 1, args.length)
                )
        );
    }

    private boolean canUseSilently(
            Player player,
            PlayerData playerData,
            IslandSubcommand subcommand
    ) {

        if (!subcommand.requiresIsland()) {
            return true;
        }

        if (!playerData.hasIsland()) {
            return false;
        }

        Island island = plugin.getIslandService()
                .getIsland(playerData.getIslandId())
                .orElse(null);

        if (island == null) {
            return false;
        }

        IslandPermission permission = subcommand.requiredPermission();

        return permission == null
                || plugin.getIslandService()
                .getAccessService()
                .hasPermission(player, island, permission);
    }
}
