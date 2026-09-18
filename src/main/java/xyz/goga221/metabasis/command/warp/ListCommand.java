package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** {@code /warp list [page]} and {@code /warp list group <groupName> [page]} */
public final class ListCommand extends BaseCommand {

    private static final int LIST_PAGE_SIZE = 8;

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("list")
                .withOptionalArguments(new IntegerArgument("page", 1))
                .executesPlayer((player, args) -> {
                    int page = (int) args.getOptional("page").orElse(1);
                    handle(player, null, page);
                })
                .withSubcommand(new CommandAPICommand("group")
                        .withArguments(new StringArgument("groupName").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.listGroupSuggestions())))
                        .withOptionalArguments(new IntegerArgument("page", 1))
                        .executesPlayer((player, args) -> {
                            String groupName = (String) args.getUnchecked("groupName");
                            int page = (int) args.getOptional("page").orElse(1);
                            handle(player, groupName, page);
                        }));
    }

    private void handle(Player player, String groupFilter, int page) {
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
