package xyz.goga221.metabasis.warp;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Shared "sort by group, then name" and "filter" logic for anywhere warps are listed — the
 * {@code /warp list} command, the player warp browser, and the admin warp list — so the three
 * stay consistent as the number of warps grows.
 */
public final class WarpFilter {

    public static final String PUBLIC_TOKEN = "public";
    private static final String NONE_TOKEN = "none";

    private static final Comparator<Warp> BY_GROUP_THEN_NAME = Comparator
            .comparing((Warp warp) -> warp.getGroupName() == null ? 1 : 0)
            .thenComparing(warp -> warp.getGroupName() == null ? "" : warp.getGroupName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Warp::getName, String.CASE_INSENSITIVE_ORDER);

    private WarpFilter() {
    }

    /**
     * Sorts by group (alphabetically, ungrouped warps last), then by warp name. When
     * {@code groupQuery} is non-blank, keeps only warps whose group name contains it
     * (case-insensitive), or only ungrouped warps if it's {@code public}/{@code none}.
     */
    public static List<Warp> sorted(Collection<Warp> warps, String groupQuery) {
        return warps.stream()
                .filter(matchingGroup(groupQuery))
                .sorted(BY_GROUP_THEN_NAME)
                .toList();
    }

    /**
     * Sorts the same way as {@link #sorted}, but when {@code query} is non-blank keeps warps
     * whose name <em>or</em> group name contains it (case-insensitive), or only ungrouped warps
     * if it's {@code public}/{@code none}. Used by the GUI search box, where a staff member may
     * not remember whether what they typed was a warp's name or its group.
     */
    public static List<Warp> search(Collection<Warp> warps, String query) {
        return warps.stream()
                .filter(matchingNameOrGroup(query))
                .sorted(BY_GROUP_THEN_NAME)
                .toList();
    }

    private static Predicate<Warp> matchingGroup(String groupQuery) {
        if (groupQuery == null || groupQuery.isBlank()) {
            return warp -> true;
        }
        String needle = groupQuery.toLowerCase(Locale.ROOT);
        if (PUBLIC_TOKEN.equals(needle) || NONE_TOKEN.equals(needle)) {
            return warp -> warp.getGroupName() == null;
        }
        return warp -> warp.getGroupName() != null && warp.getGroupName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static Predicate<Warp> matchingNameOrGroup(String query) {
        if (query == null || query.isBlank()) {
            return warp -> true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        if (PUBLIC_TOKEN.equals(needle) || NONE_TOKEN.equals(needle)) {
            return warp -> warp.getGroupName() == null;
        }
        return warp -> warp.getName().toLowerCase(Locale.ROOT).contains(needle)
                || (warp.getGroupName() != null && warp.getGroupName().toLowerCase(Locale.ROOT).contains(needle));
    }

    /** The warp's group label for display — its group name, or "Public" if ungrouped. */
    public static String groupLabel(Warp warp) {
        return warp.getGroupName() == null ? "Public" : warp.getGroupName();
    }
}
