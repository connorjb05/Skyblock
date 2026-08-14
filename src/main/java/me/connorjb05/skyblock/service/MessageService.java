package me.connorjb05.skyblock.service;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.util.SmallCapsTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageService implements Service {

    /*
     * Supports:
     *
     * &0 - &9
     * &a - &f
     * &k - &o
     * &r
     */
    private static final Pattern LEGACY_PATTERN =
            Pattern.compile(
                    "(?i)&([0-9A-FK-OR])"
            );

    private final MiniMessage miniMessage;

    public MessageService() {
        miniMessage = MiniMessage.builder()
                .editTags(tags -> tags.resolver(
                        TagResolver.resolver(
                                "smallcaps",
                                (arguments, context) ->
                                        new SmallCapsTag()
                        )
                ))
                .build();
    }

    public Component parse(
            String message
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {
            return Component.empty();
        }

        return miniMessage.deserialize(
                convertLegacyCodes(
                        message
                )
        );
    }

    public Component parse(
            String message,
            TagResolver... resolvers
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {
            return Component.empty();
        }

        return miniMessage.deserialize(
                convertLegacyCodes(
                        message
                ),
                resolvers
        );
    }

    public void send(
            CommandSender sender,
            String message
    ) {

        sender.sendMessage(
                parse(
                        message
                )
        );
    }

    public void send(
            CommandSender sender,
            String message,
            TagResolver... resolvers
    ) {

        sender.sendMessage(
                parse(
                        message,
                        resolvers
                )
        );
    }

    /*
     * Converts legacy ampersand color codes
     * into MiniMessage formatting.
     */
    private String convertLegacyCodes(
            String message
    ) {

        Matcher matcher =
                LEGACY_PATTERN.matcher(
                        message
                );

        StringBuffer result =
                new StringBuffer();

        while (matcher.find()) {

            char code =
                    Character.toLowerCase(
                            matcher.group(1)
                                    .charAt(0)
                    );

            String replacement =
                    switch (code) {

                        case '0' ->
                                "<reset><black>";

                        case '1' ->
                                "<reset><dark_blue>";

                        case '2' ->
                                "<reset><dark_green>";

                        case '3' ->
                                "<reset><dark_aqua>";

                        case '4' ->
                                "<reset><dark_red>";

                        case '5' ->
                                "<reset><dark_purple>";

                        case '6' ->
                                "<reset><gold>";

                        case '7' ->
                                "<reset><gray>";

                        case '8' ->
                                "<reset><dark_gray>";

                        case '9' ->
                                "<reset><blue>";

                        case 'a' ->
                                "<reset><green>";

                        case 'b' ->
                                "<reset><aqua>";

                        case 'c' ->
                                "<reset><red>";

                        case 'd' ->
                                "<reset><light_purple>";

                        case 'e' ->
                                "<reset><yellow>";

                        case 'f' ->
                                "<reset><white>";

                        case 'k' ->
                                "<obfuscated>";

                        case 'l' ->
                                "<bold>";

                        case 'm' ->
                                "<strikethrough>";

                        case 'n' ->
                                "<underlined>";

                        case 'o' ->
                                "<italic>";

                        case 'r' ->
                                "<reset>";

                        default ->
                                "";
                    };

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            replacement
                    )
            );
        }

        matcher.appendTail(
                result
        );

        return result.toString();
    }

    @Override
    public void onEnable(
            Skyblock plugin
    ) {
    }

    @Override
    public void onDisable(
            Skyblock plugin
    ) {
    }
}