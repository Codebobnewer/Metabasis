package com.goga221.metabasis.warp;

import com.goga221.metabasis.location.LocationSnapshot;
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
}
