package me.connorjb05.skyblock.command.island;

import me.connorjb05.skyblock.model.island.permission.IslandPermission;

import java.util.List;
import java.util.Set;

public interface IslandSubcommand {

    String name();

    String description();

    void execute(IslandCommandContext context);

    default Set<String> aliases() {
        return Set.of();
    }

    default boolean requiresIsland() {
        return false;
    }

    default IslandPermission requiredPermission() {
        return null;
    }

    default int minimumArgs() {
        return 0;
    }

    default String usage() {
        return "/is " + name();
    }

    default List<String> tabComplete(IslandCommandContext context) {
        return List.of();
    }
}
