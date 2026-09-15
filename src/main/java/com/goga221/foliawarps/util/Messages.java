package com.goga221.foliawarps.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Messages() {
    }

    public static void send(Audience audience, String miniMessageTemplate, TagResolver... placeholders) {
        audience.sendMessage(MINI_MESSAGE.deserialize(miniMessageTemplate, placeholders));
    }

    /** Wraps untrusted text (e.g. a player-chosen warp name) as plain text, not MiniMessage markup. */
    public static TagResolver name(String value) {
        return Placeholder.unparsed("name", value);
    }
}
