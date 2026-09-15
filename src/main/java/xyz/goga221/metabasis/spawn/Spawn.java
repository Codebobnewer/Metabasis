package xyz.goga221.metabasis.spawn;

import xyz.goga221.metabasis.location.LocationSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

import java.util.UUID;

@Getter
@With
@AllArgsConstructor
public class Spawn {

    private final String worldName;
    private final LocationSnapshot location;
    private final UUID setBy;
    private final long updatedAt;
    /** The permission required to teleport to this world's spawn, or null if it's public. */
    private final String permission;
}
