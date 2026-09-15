package xyz.goga221.metabasis.util;

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

    /** Wraps untrusted text (e.g. a player-chosen group name) as plain text, not MiniMessage markup. */
    public static TagResolver group(String value) {
        return Placeholder.unparsed("group", value);
    }

    /** Wraps a raw exception message (e.g. a validation failure) as plain text for the shared "error.generic" template. */
    public static TagResolver message(String value) {
        return Placeholder.unparsed("message", value);
    }

    /** A one-off unparsed placeholder under an arbitrary key, for values with no dedicated helper. */
    public static TagResolver of(String key, String value) {
        return Placeholder.unparsed(key, value);
    }
}
