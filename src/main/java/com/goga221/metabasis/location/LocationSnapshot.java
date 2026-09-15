package com.goga221.metabasis.location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

@Getter
@AllArgsConstructor
public class LocationSnapshot {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public static LocationSnapshot from(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Location has no world");
        }
        return new LocationSnapshot(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Location toBukkitLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
