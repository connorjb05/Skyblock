package me.connorjb05.skyblock.service.island.deletion.wiper;

import me.connorjb05.skyblock.model.island.IslandDeletion;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface IslandWiper {
    CompletableFuture<Void> wipe(IslandDeletion deletion);
}
