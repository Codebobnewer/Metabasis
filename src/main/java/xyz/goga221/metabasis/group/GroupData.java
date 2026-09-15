package xyz.goga221.metabasis.group;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** A loaded group together with the warps embedded in its file. */
@Getter
@AllArgsConstructor
public class GroupData {

    private final Group group;
    private final List<GroupedWarp> warps;
}
