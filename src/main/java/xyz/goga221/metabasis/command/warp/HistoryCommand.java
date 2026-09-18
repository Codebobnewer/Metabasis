package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.history.WarpHistoryEntry;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** {@code /warp history <name> [count]} */
public final class HistoryCommand extends BaseCommand {

    private static final int DEFAULT_HISTORY_COUNT = 10;
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("history")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .withOptionalArguments(new IntegerArgument("count", 1, 50))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    int count = (int) args.getOptional("count").orElse(DEFAULT_HISTORY_COUNT);
                    handle(player, name, count);
                });
    }

    private void handle(Player player, String rawName, int count) {
        String name = WarpService.normalize(rawName);
        Services.getHistoryRepository().getRecentEvents(name, count, entries -> {
            if (entries.isEmpty()) {
                Services.getMessageService().send(player, "warp.history.empty", Messages.name(name));
                return;
            }
            Services.getMessageService().send(player, "warp.history.header", Messages.name(name));
            for (WarpHistoryEntry entry : entries) {
                Services.getMessageService().send(player, "warp.history.entry",
                        Messages.of("type", entry.eventType()),
                        Messages.of("actor", describeActor(entry.actor())),
                        Messages.of("time", HISTORY_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))));
            }
        }, throwable -> Services.getMessageService().send(player, "warp.history.failed", Messages.name(name)));
    }

    private static String describeActor(UUID actor) {
        if (actor == null) {
            return "unknown";
        }
        String name = Bukkit.getOfflinePlayer(actor).getName();
        return name != null ? name : actor.toString();
    }
}
