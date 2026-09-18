package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;

import java.util.stream.Stream;

/** Suggestion providers and shared tokens used by more than one leaf command in this package. */
public final class WarpCommandSupport {

    public static final String NONE_GROUP_TOKEN = "none";
    public static final String OVERRIDE_TOKEN = "override";

    private WarpCommandSupport() {
    }

    public static String[] warpNames() {
        return Services.getWarpService().getAll().stream()
                .map(Warp::getName)
                .toArray(String[]::new);
    }

    public static String[] groupNameSuggestions() {
        return Stream.concat(
                        Services.getGroupService().getAll().stream().map(Group::getName),
                        Stream.of(NONE_GROUP_TOKEN))
                .toArray(String[]::new);
    }

    public static String[] listGroupSuggestions() {
        return Stream.concat(Stream.of(WarpFilter.PUBLIC_TOKEN), Services.getGroupService().getAll().stream().map(Group::getName))
                .toArray(String[]::new);
    }
}
