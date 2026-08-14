package me.connorjb05.skyblock.command.island;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.island.Island;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class IslandCommandUtil {

    private IslandCommandUtil() {

    }

    public static void sync(Skyblock plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static Long parseIslandId(Player player, String input) {
        try {
            long islandId = Long.parseLong(input);

            if (islandId < 0) {
                throw new NumberFormatException();
            }

            return islandId;

        } catch (NumberFormatException e) {
            IslandCommandStatus.INVALID_ISLAND_ID.sendMessage(player);
            return null;
        }
    }

    public static String leaderName(Island island) {
        Player onlineOwner = Bukkit.getPlayer(island.getLeaderId());

        if (onlineOwner != null) {
            return onlineOwner.getName();
        }

        String name = Bukkit.getOfflinePlayer(island.getLeaderId()).getName();
        return name == null ? "Unknown" : name;
    }

    public static String prettyEnum(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            builder.append(part.substring(1));
        }

        return builder.toString();
    }

    public static List<String> filter(Collection<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);

        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    public static void usage(Player player, String usage) {
        IslandCommandStatus.USAGE.sendMessage(
                player,
                Placeholder.unparsed("usage", usage)
        );
    }
}
