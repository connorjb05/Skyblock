package me.connorjb05.skyblock.service.island.permission;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.database.repository.IslandPermissionRepository;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.membership.IslandRole;
import me.connorjb05.skyblock.model.island.permission.IslandPermission;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class IslandPermissionService {

    private final Skyblock plugin;
    private final IslandPermissionRepository repository;
    private final IslandAccessService accessService;

    public IslandPermissionService(
            Skyblock plugin,
            IslandPermissionRepository repository,
            IslandAccessService accessService
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.accessService = accessService;
    }

    /*
    Changes the minimum island role
    required for a permission.
     */
    public CompletableFuture<IslandPermissionResult> setPermissionLevel(
            Player player,
            Island island,
            IslandPermission permission,
            IslandRole requiredRole
    ) {

        if (!accessService.checkPermission(
                player,
                island,
                IslandPermission.MANAGE_PERMISSIONS
        )) {
            return CompletableFuture.completedFuture(
                    IslandPermissionResult.failure(
                            IslandPermissionStatus.NO_PERMISSION
                    )
            );
        }

        int permissionLevel = requiredRole.getPermissionLevel();

        int currentLevel = island.getPermissionData()
                .getPermissionLevel(permission);

        if (currentLevel == permissionLevel) {
            return CompletableFuture.completedFuture(
                    IslandPermissionResult.success(
                            IslandPermissionStatus.UPDATE_SUCCESS
                    )
            );
        }

        /*
        Repository automatically removes
        the row when the value is default.
         */
        return repository.setLevel(
                        island.getIslandId(),
                        permission,
                        permissionLevel
                )
                .thenCompose(ignored -> runSync(() -> island.getPermissionData()
                                .setPermissionLevel(
                                        permission,
                                        permissionLevel
                                )
                        )
                )
                .thenCompose(
                        ignored -> {

                            IslandPermissionStatus status;

                            if (permissionLevel == permission.getDefaultPermissionLevel()) {

                                status = IslandPermissionStatus.RESET_SUCCESS;

                            } else {

                                status = IslandPermissionStatus.UPDATE_SUCCESS;
                            }

                            return sendResult(
                                    player,
                                    IslandPermissionResult.success(status),
                                    permission,
                                    requiredRole
                            );
                        }
                )
                .exceptionallyCompose(throwable -> {

                    plugin.getLogger().severe(
                            "Failed to update island permission "
                                    + permission.name()
                                    + " for island "
                                    + island.getIslandId()
                    );

                    throwable.printStackTrace();

                    return sendResult(
                            player,
                            IslandPermissionResult.failure(
                                    IslandPermissionStatus.FAILURE
                            )
                    );
                });
    }

    /*
    Resets one permission back
    to its default role.
     */
    public CompletableFuture<IslandPermissionResult> resetPermission(
            Player player,
            Island island,
            IslandPermission permission
    ) {

        if (!accessService.checkPermission(
                player,
                island,
                IslandPermission.MANAGE_PERMISSIONS
        )) {
            return CompletableFuture.completedFuture(
                    IslandPermissionResult.failure(
                            IslandPermissionStatus.NO_PERMISSION
                    )
            );
        }

        if (!island.getPermissionData().hasOverride(permission)) {
            return CompletableFuture.completedFuture(
                    IslandPermissionResult.success(
                            IslandPermissionStatus.RESET_SUCCESS
                    )
            );
        }

        return repository.delete(
                        island.getIslandId(),
                        permission
                )
                .thenCompose(ignored -> runSync(() -> island.getPermissionData()
                        .resetPermission(
                                permission
                        )
                ))
                .thenCompose(ignored -> sendResult(
                        player,
                        IslandPermissionResult.success(IslandPermissionStatus.RESET_SUCCESS),
                        permission,
                        permission.getDefaultRole()
                ))
                .exceptionallyCompose(throwable -> {

                    throwable.printStackTrace();

                    return sendResult(
                            player,
                            IslandPermissionResult.failure(
                                    IslandPermissionStatus.FAILURE
                            )
                    );
                });
    }

    /*
        Resets every customized permission
        back to its default.
         */
    public CompletableFuture<IslandPermissionResult> resetAll(
            Player player,
            Island island
    ) {

        if (!accessService.checkPermission(
                player,
                island,
                IslandPermission.MANAGE_PERMISSIONS
        )) {

            return CompletableFuture.completedFuture(
                    IslandPermissionResult.failure(
                            IslandPermissionStatus.NO_PERMISSION
                    )
            );
        }

        return repository.deleteAll(island.getIslandId())
                .thenCompose(ignored -> runSync(() -> island.getPermissionData()
                                .reset()
                        )
                )
                .thenCompose(ignored -> sendResult(
                        player,
                        IslandPermissionResult.success(IslandPermissionStatus.RESET_ALL_SUCCESS)
                        )
                )
                .exceptionallyCompose(
                        throwable -> {

                            throwable.printStackTrace();

                            return sendResult(
                                    player,
                                    IslandPermissionResult.failure(
                                            IslandPermissionStatus.FAILURE
                                    )
                            );
                        }
                );
    }

    private CompletableFuture<IslandPermissionResult> sendResult(
            Player player,
            IslandPermissionResult result,
            IslandPermission permission,
            IslandRole role
    ) {

        TagResolver permissionResolver =
                Placeholder.unparsed(
                        "permission",
                        formatEnumName(
                                permission.name()
                        )
                );

        TagResolver roleResolver =
                Placeholder.unparsed(
                        "role",
                        formatEnumName(
                                role.name()
                        )
                );

        return sendResult(
                player,
                result,
                permissionResolver,
                roleResolver
        );
    }

    private CompletableFuture<IslandPermissionResult> sendResult(
            Player player,
            IslandPermissionResult result,
            TagResolver... resolvers
    ) {

        return runSync(() -> {

            if (!player.isOnline()) {
                return;
            }

            result.status()
                    .sendMessage(
                            player,
                            resolvers
                    );
        }).thenApply(ignored -> result);
    }

    private CompletableFuture<Void> runSync(
            Runnable task
    ) {

        if (Bukkit.isPrimaryThread()) {

            task.run();

            return CompletableFuture.completedFuture(
                    null
            );
        }

        CompletableFuture<Void> future =
                new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {

                task.run();

                future.complete(
                        null
                );

            } catch (Throwable throwable) {

                future.completeExceptionally(
                        throwable
                );
            }
        });

        return future;
    }

    private String formatEnumName(
            String value
    ) {

        String[] parts =
                value.toLowerCase()
                        .split("_");

        StringBuilder builder =
                new StringBuilder();

        for (String part : parts) {

            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            builder.append(
                    part.substring(1)
            );
        }

        return builder.toString();
    }
}
