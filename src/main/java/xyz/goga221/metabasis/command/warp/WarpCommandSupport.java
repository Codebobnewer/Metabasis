package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/** Suggestion providers, shared tokens, and the {@code list} render logic used by more than one leaf command in this package. */
public final class WarpCommandSupport {

    public static final String NONE_GROUP_TOKEN = "none";
    public static final String OVERRIDE_TOKEN = "override";
    private static final int LIST_PAGE_SIZE = 8;

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

    /** Shared by {@link ListCommand} and {@link ListGroupCommand}, which must be separate registrations under {@code list} — see their class comments for why. */
    public static void renderList(Player player, String groupFilter, int page) {
        Collection<Warp> all = Services.getWarpService().getAll();
        if (all.isEmpty()) {
            Services.getMessageService().send(player, "warp.list.empty");
            return;
        }

        List<Warp> filtered = WarpFilter.sorted(all, groupFilter);
        if (filtered.isEmpty()) {
            Services.getMessageService().send(player, "warp.list.filter-empty", Messages.name(groupFilter));
            return;
        }

        int totalPages = Math.max(1, (filtered.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE);
        if (page < 1 || page > totalPages) {
            Services.getMessageService().send(player, "warp.list.invalid-page",
                    Messages.of("page", String.valueOf(page)),
                    Messages.of("pages", String.valueOf(totalPages)));
            return;
        }

        Services.getMessageService().send(player, "warp.list.page-header",
                Messages.of("page", String.valueOf(page)),
                Messages.of("pages", String.valueOf(totalPages)));

        int fromIndex = (page - 1) * LIST_PAGE_SIZE;
        int toIndex = Math.min(filtered.size(), fromIndex + LIST_PAGE_SIZE);
        String lastGroup = null;
        for (Warp warp : filtered.subList(fromIndex, toIndex)) {
            String group = WarpFilter.groupLabel(warp);
            if (!group.equals(lastGroup)) {
                Services.getMessageService().send(player, "warp.list.group-header", Messages.group(group));
                lastGroup = group;
            }
            Component suffix = warp.isEnabled() ? Component.empty() : Component.text(" [disabled]", NamedTextColor.RED);
            Services.getMessageService().send(player, "warp.list.entry", Messages.name(warp.getName()), Placeholder.component("suffix", suffix));
        }

        if (totalPages > 1) {
            Services.getMessageService().send(player, "warp.list.footer");
        }
    }
}
