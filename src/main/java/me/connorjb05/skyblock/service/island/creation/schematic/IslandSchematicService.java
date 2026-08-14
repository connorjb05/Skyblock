package me.connorjb05.skyblock.service.island.creation.schematic;

import me.connorjb05.skyblock.model.island.Island;

import java.util.concurrent.CompletableFuture;

public interface IslandSchematicService {
    CompletableFuture<IslandPasteResult> paste(Island island);
}
