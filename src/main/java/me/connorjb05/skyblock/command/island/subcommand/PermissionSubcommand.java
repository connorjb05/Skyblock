package me.connorjb05.skyblock.command.island.subcommand;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.command.island.IslandCommandContext;
import me.connorjb05.skyblock.command.island.IslandCommandStatus;
import me.connorjb05.skyblock.command.island.IslandCommandUtil;
import me.connorjb05.skyblock.command.island.IslandSubcommand;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.*;
import java.util.stream.Stream;

public class PermissionSubcommand implements IslandSubcommand {

    private final Skyblock plugin;

    public PermissionSubcommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "permissions";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("permission", "perms");
    }

    @Override
    public String description() {
        return "Manage island permissions";
    }

    @Override
    public boolean requiresIsland() {
        return true;
    }

    @Override
    public IslandPermission requiredPermission() {
        return IslandPermission.MANAGE_PERMISSIONS;
    }

    @Override
    public String usage() {
        return "/is permissions <permission> <role|reset>";
    }

    @Override
    public void execute(IslandCommandContext context) {
        if (context.args().length == 0) {
            plugin.getMessageService().send(
                    context.player(),
                    "<#FF9F1C><bold>Island Permissions</bold>"
            );

            for (IslandPermission permission : IslandPermission.values()) {
                int level = context.island()
                        .getPermissionData()
                        .getPermissionLevel(permission);

                IslandRole role = IslandRole.fromPermissionLevel(level);

                plugin.getMessageService().send(
                        context.player(),
                        "<#FFD84D>"
                                + IslandCommandUtil.prettyEnum(permission.name())
                                + ": <white>"
                                + IslandCommandUtil.prettyEnum(role.name())
                                + "+"
                );
            }

            return;
        }

        if (context.args().length < 2) {
            IslandCommandUtil.usage(context.player(), usage());
            return;
        }

        IslandPermission permission;

        try {
            permission = IslandPermission.valueOf(normalize(context.args()[0]));

        } catch (IllegalArgumentException e) {
            IslandCommandStatus.INVALID_PERMISSION.sendMessage(
                    context.player(),
                    Placeholder.unparsed("permission", context.args()[0])
            );
            return;
        }

        String roleInput = context.args()[1];

        if (roleInput.equalsIgnoreCase("reset")) {
            plugin.getIslandService()
                    .getPermissionService()
                    .resetPermission(
                            context.player(),
                            context.island(),
                            permission
                    );

            return;
        }

        IslandRole role;

        try {
            role = IslandRole.valueOf(normalize(roleInput));

        } catch (IllegalArgumentException e) {
            IslandCommandStatus.INVALID_ROLE.sendMessage(
                    context.player(),
                    Placeholder.unparsed("role", roleInput)
            );
            return;
        }

        plugin.getIslandService()
                .getPermissionService()
                .setPermissionLevel(
                        context.player(),
                        context.island(),
                        permission,
                        role
                );
    }

    @Override
    public List<String> tabComplete(IslandCommandContext context) {
        if (context.args().length == 1) {
            List<String> permissions = Arrays.stream(IslandPermission.values())
                    .map(permission -> permission.name().toLowerCase(Locale.ROOT))
                    .toList();

            return IslandCommandUtil.filter(permissions, context.args()[0]);
        }

        if (context.args().length == 2) {
            List<String> roles = Stream.concat(
                            Stream.of("reset"),
                            Arrays.stream(IslandRole.values())
                                    .map(role -> role.name().toLowerCase(Locale.ROOT))
                    )
                    .toList();

            return IslandCommandUtil.filter(roles, context.args()[1]);
        }

        return List.of();
    }

    private String normalize(String value) {
        return value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }
}
