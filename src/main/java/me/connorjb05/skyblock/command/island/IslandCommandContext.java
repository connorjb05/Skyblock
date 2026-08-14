package me.connorjb05.skyblock.command.island;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.model.PlayerData;
import me.connorjb05.skyblock.model.island.Island;
import org.bukkit.entity.Player;

public record IslandCommandContext(
        Skyblock plugin,
        Player player,
        PlayerData playerData,
        Island island,
        String[] args
) {

}
