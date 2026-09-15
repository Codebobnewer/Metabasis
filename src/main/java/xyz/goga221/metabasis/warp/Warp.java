package xyz.goga221.metabasis.warp;

import xyz.goga221.metabasis.location.LocationSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

import java.util.UUID;

@Getter
@With
@AllArgsConstructor
public class Warp {

    private final String name;
    private final LocationSnapshot location;
    private final UUID creator;
    private final long createdAt;
    /** The group this warp is restricted to, or null if it's public. */
    private final String groupName;
    /** While false, nobody (including operators) can teleport to this warp. */
    private final boolean enabled;
    /** Title fade-in/stay/fade-out durations (ticks) shown on arrival; all 0 means no title. */
    private final int fadeInTicks;
    private final int stayTicks;
    private final int fadeOutTicks;
    /** Seconds the player must stand still before teleporting; 0 means instant. */
    private final int warmupSeconds;
}
