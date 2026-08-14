package me.connorjb05.skyblock.util;

import me.connorjb05.skyblock.Skyblock;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Broadcast {

    private Broadcast(){}

    public static void send(String message) {

        if (Skyblock.get() == null || Skyblock.get().getMessageService() == null) {
            throw new IllegalStateException(
                    "Plugin has not loaded fully yet!"
            );
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Skyblock.get()
                    .getMessageService()
                    .send(
                            player,
                            message
                    );
        }
    }

    public static void send(String message, TagResolver... resolvers) {

        if (Skyblock.get() == null || Skyblock.get().getMessageService() == null) {
            throw new IllegalStateException(
                    "Plugin has not loaded fully yet!"
            );
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Skyblock.get()
                    .getMessageService()
                    .send(
                            player,
                            message,
                            resolvers
                    );
        }
    }

}
