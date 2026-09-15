package xyz.goga221.metabasis.location;

import org.bukkit.configuration.ConfigurationSection;

public final class YamlLocationCodec {

    private YamlLocationCodec() {
    }

    public static void write(ConfigurationSection section, LocationSnapshot location) {
        section.set("world", location.getWorldName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    public static LocationSnapshot read(ConfigurationSection section) {
        return new LocationSnapshot(
                section.getString("world"),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }
}
