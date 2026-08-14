package me.connorjb05.skyblock.service.result;

import me.connorjb05.skyblock.Skyblock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public interface ResultStatus {

    Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(10L * 50L),
            Duration.ofMillis(40L * 50L),
            Duration.ofMillis(10L * 50L)
    );

    String getMessage();

    void setMessage(String message);

    default void sendMessage(CommandSender sender) {
        Skyblock.get()
                .getMessageService()
                .send(
                        sender,
                        getMessage()
                );
    }

    default void sendMessage(
            CommandSender sender,
            TagResolver... resolvers
    ) {
        Skyblock.get()
                .getMessageService()
                .send(
                        sender,
                        getMessage(),
                        resolvers
                );
    }

    default void sendTitle(
            Player player,
            TagResolver... resolvers
    ) {
        sendTitle(
                player,
                Component.empty(),
                resolvers
        );
    }

    default void sendSubtitle(
            Player player,
            TagResolver... resolvers
    ) {
        Component subtitle = Skyblock.get()
                .getMessageService()
                .parse(
                        getMessage(),
                        resolvers
                );

        player.showTitle(Title.title(
                Component.empty(),
                subtitle,
                TITLE_TIMES
        ));
    }

    default void sendTitle(
            Player player,
            ResultStatus subtitle,
            TagResolver... resolvers
    ) {

        Component title = Skyblock.get()
                .getMessageService()
                .parse(
                        getMessage(),
                        resolvers
                );

        Component subtitleComponent = subtitle == null
                ?
                Component.empty()
                :
                Skyblock.get()
                        .getMessageService()
                        .parse(
                                subtitle.getMessage(),
                                resolvers
                        );

        player.showTitle(Title.title(
                title,
                subtitleComponent,
                TITLE_TIMES
        ));
    }

    private void sendTitle(
            Player player,
            Component subtitle,
            TagResolver... resolvers
    ) {

        Component title = Skyblock.get()
                .getMessageService()
                .parse(
                        getMessage(),
                        resolvers
                );

        player.showTitle(Title.title(
                title,
                subtitle,
                TITLE_TIMES
        ));
    }
}
