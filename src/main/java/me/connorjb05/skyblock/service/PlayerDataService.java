package me.connorjb05.skyblock.service;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataService implements Service{

    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable(Skyblock plugin) {
        Bukkit.getOnlinePlayers().forEach(this::load);
    }

    @Override
    public void onDisable(Skyblock plugin) {
        playerDataMap.keySet().forEach(this::unload);
    }

    public CompletableFuture<PlayerData> load(Player player) {

        UUID playerId = player.getUniqueId();
        PlayerData playerData = new PlayerData(player);

        /*
        Cache immediately
         */
        playerDataMap.put(
                playerId,
                playerData
        );

        return Skyblock.get()
                .getIslandService()
                .loadPlayer(playerData)
                .thenApply(ignored -> {
                    playerData.setLoaded(true);
                    return playerData;
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        /*
                        Don't leave partially-loaded
                        data in the cache.
                         */
                        playerDataMap.remove(
                                playerId,
                                playerData
                        );
                    }
                });
    }

    public void unload(UUID uuid) {
        PlayerData playerData = playerDataMap.remove(uuid);
        playerData.close();
    }

    public PlayerData get(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public PlayerData get(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    public Map<UUID, PlayerData> getAll() {
        return playerDataMap;
    }
}
