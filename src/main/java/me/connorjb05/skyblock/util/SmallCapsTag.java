package me.connorjb05.skyblock.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.Modifying;

public class SmallCapsTag implements Modifying {

    @Override
    public Component apply(Component current, int depth) {

        if (!(current instanceof TextComponent textComponent)) {
            return current;
        }

        String content =
                textComponent.content();

        if (content.isEmpty()) {
            return current;
        }

        return textComponent.content(
                SmallCapsUtil.convert(content)
        );
    }
}
