package me.connorjb05.skyblock.listener;

import me.connorjb05.skyblock.Skyblock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final Skyblock plugin;

    public JoinQuitListener(Skyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        plugin.getPlayerDataService()
                .load(player)
                .exceptionally(throwable -> {
                    plugin.getLogger().severe(
                            "Failed to load player island data for " +
                                    player.getName() + ": " +
                                    player.getUniqueId() +
                                    ": " +
                                    throwable.getMessage()
                    );

                    throwable.printStackTrace();

                    return null;
                });
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        plugin.getPlayerDataService().unload(player.getUniqueId());
    }
}
