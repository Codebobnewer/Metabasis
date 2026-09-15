package xyz.goga221.metabasis.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * Loads command-facing message templates from {@code message.yml}, falling back to
 * {@link MessageDefaults} for any key the file doesn't define. The file is treated as an
 * override layer, not the source of truth — an admin only needs to add the keys they actually
 * want to change.
 */
public final class MessageService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final File messageFile;
    private final Logger logger;
    private YamlConfiguration config;

    public MessageService(File dataFolder, Logger logger) {
        this.messageFile = new File(dataFolder, "message.yml");
        this.logger = logger;
        reload();
    }

    /** Re-reads message.yml from disk, picking up any edits made while the server is running. */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(messageFile);
    }

    /** The raw MiniMessage template for the given key — the file's value if set, else the built-in default. */
    public String get(String key) {
        String fromFile = config.getString(key);
        if (fromFile != null) {
            return fromFile;
        }
        String fallback = MessageDefaults.ALL.get(key);
        if (fallback == null) {
            logger.warning("No default registered for message key '" + key + "' — this is a plugin bug, not a config problem.");
            return "<red>Missing message: " + key + "</red>";
        }
        return fallback;
    }

    public void send(Audience audience, String key, TagResolver... placeholders) {
        audience.sendMessage(MINI_MESSAGE.deserialize(get(key), placeholders));
    }

    /** Parses the given key's template into a Component, for callers that need more than a direct send (e.g. a Title). */
    public Component parse(String key, TagResolver... placeholders) {
        return MINI_MESSAGE.deserialize(get(key), placeholders);
    }
}
