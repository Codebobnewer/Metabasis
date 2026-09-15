package xyz.goga221.metabasis.group;

import xyz.goga221.metabasis.location.LocationSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * A warp's persisted fields as stored inside its owning group's file. Deliberately independent
 * of {@code warp.Warp} so this package never has to depend on the warp package (warp already
 * depends on group, for permission checks — a dependency back the other way would be circular).
 */
@Getter
@AllArgsConstructor
public class GroupedWarp {

    private final String name;
    private final LocationSnapshot location;
    private final UUID creator;
    private final long createdAt;
    private final boolean enabled;
    private final int fadeInTicks;
    private final int stayTicks;
    private final int fadeOutTicks;
    private final int warmupSeconds;
}
