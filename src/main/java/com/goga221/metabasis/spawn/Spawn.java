package com.goga221.metabasis.spawn;

import com.goga221.metabasis.location.LocationSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class Spawn {

    private final String worldName;
    private final LocationSnapshot location;
    private final UUID setBy;
    private final long updatedAt;
}
