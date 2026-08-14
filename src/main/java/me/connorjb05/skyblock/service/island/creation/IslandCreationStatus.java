package me.connorjb05.skyblock.service.island.creation;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.service.result.ResultStatus;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public enum IslandCreationStatus implements ResultStatus {
    SUCCESS,
    ALREADY_IN_ISLAND,
    SLOT_LIMIT_REACHED;

    private String message;

    @Override
    public void setMessage(
            String message
    ) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
