package xyz.goga221.metabasis.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@With
@AllArgsConstructor
public class Group {

    private final String name;
    private final String permission;
    /** Free-text admin note about what this group is for, or null if none was set. */
    private final String description;
    /** While false, every warp in this group is inaccessible regardless of permission. */
    private final boolean enabled;
}
