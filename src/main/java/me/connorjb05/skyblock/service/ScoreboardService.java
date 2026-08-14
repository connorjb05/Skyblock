package me.connorjb05.skyblock.service;

import fr.mrmicky.fastboard.adventure.FastBoard;
import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.service.config.scoreboard.ConfigBoard;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class ScoreboardService implements Service {

    private static final long UPDATE_INTERVAL = 20L;

    private BukkitTask updateTask;

    @Override
    public void onEnable(Skyblock plugin) {
        updateTask = Bukkit.getScheduler()
                .runTaskTimer(
                        plugin,
                        this::update,
                        0L,
                        UPDATE_INTERVAL
                );
    }

    @Override
    public void onDisable(Skyblock plugin) {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void update() {

        for (PlayerData playerData : Skyblock.get().getPlayerDataService().getAll().values()) {
            individual(playerData);
        }

    }

    private void individual(PlayerData playerData) {

        if (playerData == null) {
            return;
        }

        FastBoard board = playerData.getScoreboard();

        if (board == null) {
            return;
        }

        if (!playerData.hasIsland()) {
            touchScoreboard(
                    board,
                    Skyblock.get()
                            .getConfigService()
                            .getNoIslandBoard(),

                    Placeholder.unparsed(
                            "player",
                            playerData.getPlayer().getName()
                    ),

                    Placeholder.unparsed(
                            "player",
                            playerData.getPlayer().getName()
                    )

            );
            return;
        }

        Island island = Skyblock.get()
                .getIslandService()
                .getIsland(playerData.getIslandId())
                .orElse(null);

        /*
        PlayerData says they have an island,
        but it isn't currently available
        in the active island cache.
         */
        if (island == null) {
            touchScoreboard(
                    board,
                    Skyblock.get()
                            .getConfigService()
                            .getLoadingBoard()
            );
            return;
        }

        touchScoreboard(
                board,
                Skyblock.get().getConfigService().getIslandBoard(),

                Placeholder.unparsed(
                        "player",
                        playerData.getPlayer().getName()
                ),

                Placeholder.unparsed(
                        "island_id",
                        String.valueOf(
                                island.getIslandId()
                        )
                ),

                Placeholder.unparsed(
                        "island_role",
                        playerData.getIslandMember().role().name()
                ),

                Placeholder.unparsed(
                        "island_members",
                        String.valueOf(
                                island.getMemberCount()
                        )
                )
        );
    }

    private void touchScoreboard(
            FastBoard board,
            ConfigBoard configBoard,
            TagResolver... resolvers
    ) {

        MessageService messageService =
                Skyblock.get()
                        .getMessageService();

        board.updateTitle(
                messageService.parse(
                        configBoard.title(),
                        resolvers
                )
        );

        board.updateLines(
                configBoard.lines()
                        .stream()
                        .map(line ->
                                messageService.parse(
                                        line,
                                        resolvers
                                )
                        )
                        .toList()
        );
    }
}
