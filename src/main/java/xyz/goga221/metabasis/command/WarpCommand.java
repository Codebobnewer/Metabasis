package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.gui.AdminMenuGui;
import xyz.goga221.metabasis.gui.GuiContext;
import xyz.goga221.metabasis.gui.PlayerWarpGui;
import xyz.goga221.metabasis.history.WarpHistoryEntry;
import xyz.goga221.metabasis.history.WarpHistoryRepository;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class WarpCommand {

    private static final String NONE_GROUP_TOKEN = "none";
    private static final String OVERRIDE_TOKEN = "override";
    private static final int DEFAULT_HISTORY_COUNT = 10;
    private static final int LIST_PAGE_SIZE = 8;
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final WarpService warpService;
    private final GroupService groupService;
    private final WarpHistoryRepository history;
    private final MessageService messages;
    private final GuiContext guiContext;

    public WarpCommand(WarpService warpService, GroupService groupService, WarpHistoryRepository history, MessageService messages, GuiContext guiContext) {
        this.warpService = warpService;
        this.groupService = groupService;
        this.history = history;
        this.messages = messages;
        this.guiContext = guiContext;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("warp")
                .then(new LiteralArgument("create")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleCreate(player, name);
                                })))
                .then(new LiteralArgument("move")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleMove(player, name);
                                })))
                .then(new LiteralArgument("del")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleDelete(player, name);
                                })))
                .then(new LiteralArgument("group")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("warpName")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .then(new StringArgument("groupName")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNameSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String warpName = (String) args.getUnchecked("warpName");
                                            String groupName = (String) args.getUnchecked("groupName");
                                            handleGroupAssign(player, warpName, groupName);
                                        }))))
                .then(new LiteralArgument("enable")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSetEnabled(player, name, true);
                                })))
                .then(new LiteralArgument("disable")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSetEnabled(player, name, false);
                                })))
                .then(new LiteralArgument("fade")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .then(new IntegerArgument("fadeIn", 0)
                                        .then(new IntegerArgument("stay", 0)
                                                .then(new IntegerArgument("fadeOut", 0)
                                                        .executesPlayer((player, args) -> {
                                                            String name = (String) args.getUnchecked("name");
                                                            int fadeIn = (int) args.getUnchecked("fadeIn");
                                                            int stay = (int) args.getUnchecked("stay");
                                                            int fadeOut = (int) args.getUnchecked("fadeOut");
                                                            handleSetFade(player, name, fadeIn, stay, fadeOut);
                                                        }))))))
                .then(new LiteralArgument("warmup")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .then(new IntegerArgument("seconds", 0)
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            int seconds = (int) args.getUnchecked("seconds");
                                            handleSetWarmup(player, name, seconds);
                                        }))))
                .then(new LiteralArgument("history")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleHistory(player, name, DEFAULT_HISTORY_COUNT);
                                })
                                .then(new IntegerArgument("count", 1, 50)
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            int count = (int) args.getUnchecked("count");
                                            handleHistory(player, name, count);
                                        }))))
                .then(new LiteralArgument("gui")
                        .executesPlayer((player, args) -> {
                            new PlayerWarpGui(warpService, messages).open(player);
                        }))
                .then(new LiteralArgument("admin")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .executesPlayer((player, args) -> {
                            new AdminMenuGui(guiContext).open(player);
                        }))
                .then(new LiteralArgument("massport")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleMassport(player, name, false);
                                })
                                .then(new LiteralArgument(OVERRIDE_TOKEN)
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            handleMassport(player, name, true);
                                        }))))
                .then(new LiteralArgument("list")
                        .executesPlayer((player, args) -> {
                            handleList(player, null, 1);
                        })
                        .then(new IntegerArgument("page", 1)
                                .executesPlayer((player, args) -> {
                                    int page = (int) args.getUnchecked("page");
                                    handleList(player, null, page);
                                }))
                        .then(new LiteralArgument("group")
                                .then(new StringArgument("groupName")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> listGroupSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String groupName = (String) args.getUnchecked("groupName");
                                            handleList(player, groupName, 1);
                                        })
                                        .then(new IntegerArgument("page", 1)
                                                .executesPlayer((player, args) -> {
                                                    String groupName = (String) args.getUnchecked("groupName");
                                                    int page = (int) args.getUnchecked("page");
                                                    handleList(player, groupName, page);
                                                })))))
                .then(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                        .executesPlayer((player, args) -> {
                            String name = (String) args.getUnchecked("name");
                            handleTeleport(player, name);
                        }))
                .register(plugin);
    }

    private String[] warpNames() {
        return warpService.getAll().stream()
                .map(Warp::getName)
                .toArray(String[]::new);
    }

    private String[] groupNameSuggestions() {
        return Stream.concat(
                        groupService.getAll().stream().map(Group::getName),
                        Stream.of(NONE_GROUP_TOKEN))
                .toArray(String[]::new);
    }

    private String[] listGroupSuggestions() {
        return Stream.concat(Stream.of(WarpFilter.PUBLIC_TOKEN), groupService.getAll().stream().map(Group::getName))
                .toArray(String[]::new);
    }

    /** Only ever creates a brand-new warp — refuses if the name is already taken, unlike {@link #handleMove}. */
    private void handleCreate(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        if (warpService.get(name).isPresent()) {
            messages.send(player, "warp.create.exists", Messages.name(name));
            return;
        }

        try {
            warpService.createWarp(rawName, player.getLocation(), player.getUniqueId(), saved -> {
                if (saved) {
                    messages.send(player, "warp.create.success", Messages.name(name));
                } else {
                    messages.send(player, "warp.create.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    /** Only ever relocates an existing warp — refuses if the name doesn't exist yet, unlike {@link #handleCreate}. */
    private void handleMove(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        if (warpService.get(name).isEmpty()) {
            messages.send(player, "warp.not-found", Messages.name(name));
            return;
        }

        try {
            warpService.createWarp(rawName, player.getLocation(), player.getUniqueId(), saved -> {
                if (saved) {
                    messages.send(player, "warp.move.success", Messages.name(name));
                } else {
                    messages.send(player, "warp.move.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    private void handleDelete(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        warpService.deleteWarp(name, deleted -> {
            if (deleted) {
                messages.send(player, "warp.delete.success", Messages.name(name));
            } else {
                messages.send(player, "warp.not-found", Messages.name(name));
            }
        });
    }

    private void handleGroupAssign(Player player, String rawWarpName, String rawGroupName) {
        String warpName = WarpService.normalize(rawWarpName);
        String groupToken = GroupService.normalize(rawGroupName);
        String groupName = NONE_GROUP_TOKEN.equals(groupToken) ? null : groupToken;

        if (groupName != null && !groupService.exists(groupName)) {
            messages.send(player, "group.not-found", Messages.name(groupName));
            return;
        }

        try {
            warpService.setWarpGroup(warpName, groupName, saved -> {
                if (saved) {
                    if (groupName == null) {
                        messages.send(player, "warp.group.cleared", Messages.name(warpName));
                    } else {
                        messages.send(player, "warp.group.assigned", Messages.name(warpName), Messages.group(groupName));
                    }
                } else {
                    messages.send(player, "warp.update.failed", Messages.name(warpName));
                }
            });
        } catch (IllegalArgumentException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    private void handleSetEnabled(Player player, String rawName, boolean enabled) {
        String name = WarpService.normalize(rawName);
        try {
            warpService.setWarpEnabled(name, enabled, saved -> {
                if (saved) {
                    messages.send(player, enabled ? "warp.enable.success" : "warp.disable.success", Messages.name(name));
                } else {
                    messages.send(player, "warp.update.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    private void handleSetFade(Player player, String rawName, int fadeIn, int stay, int fadeOut) {
        String name = WarpService.normalize(rawName);
        try {
            warpService.setWarpFade(name, fadeIn, stay, fadeOut, saved -> {
                if (saved) {
                    messages.send(player, "warp.fade.success", Messages.name(name));
                } else {
                    messages.send(player, "warp.update.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    private void handleSetWarmup(Player player, String rawName, int seconds) {
        String name = WarpService.normalize(rawName);
        try {
            warpService.setWarpWarmup(name, seconds, saved -> {
                if (saved) {
                    messages.send(player, "warp.warmup.set.success", Messages.name(name), Messages.of("seconds", String.valueOf(seconds)));
                } else {
                    messages.send(player, "warp.update.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException e) {
            messages.send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }

    private void handleHistory(Player player, String rawName, int count) {
        String name = WarpService.normalize(rawName);
        history.getRecentEvents(name, count, entries -> {
            if (entries.isEmpty()) {
                messages.send(player, "warp.history.empty", Messages.name(name));
                return;
            }
            messages.send(player, "warp.history.header", Messages.name(name));
            for (WarpHistoryEntry entry : entries) {
                messages.send(player, "warp.history.entry",
                        Messages.of("type", entry.eventType()),
                        Messages.of("actor", describeActor(entry.actor())),
                        Messages.of("time", HISTORY_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))));
            }
        }, throwable -> messages.send(player, "warp.history.failed", Messages.name(name)));
    }

    private static String describeActor(UUID actor) {
        if (actor == null) {
            return "unknown";
        }
        String name = Bukkit.getOfflinePlayer(actor).getName();
        return name != null ? name : actor.toString();
    }

    private void handleMassport(Player sender, String rawName, boolean override) {
        String name = WarpService.normalize(rawName);
        Optional<Warp> warp = warpService.get(name);
        if (warp.isEmpty()) {
            messages.send(sender, "warp.not-found", Messages.name(name));
            return;
        }

        Warp target = warp.get();
        int total = 0;
        int skipped = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            total++;
            if (!override && !warpService.canAccess(player, target)) {
                skipped++;
                continue;
            }
            warpService.teleport(player, target, success -> {
            });
        }

        int teleported = total - skipped;
        messages.send(sender, "warp.massport.summary",
                Messages.name(name),
                Messages.of("teleported", String.valueOf(teleported)),
                Messages.of("total", String.valueOf(total)),
                Messages.of("skipped", String.valueOf(skipped)));
    }

    private void handleList(Player player, String groupFilter, int page) {
        Collection<Warp> all = warpService.getAll();
        if (all.isEmpty()) {
            messages.send(player, "warp.list.empty");
            return;
        }

        List<Warp> filtered = WarpFilter.sorted(all, groupFilter);
        if (filtered.isEmpty()) {
            messages.send(player, "warp.list.filter-empty", Messages.name(groupFilter));
            return;
        }

        int totalPages = Math.max(1, (filtered.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE);
        if (page < 1 || page > totalPages) {
            messages.send(player, "warp.list.invalid-page",
                    Messages.of("page", String.valueOf(page)),
                    Messages.of("pages", String.valueOf(totalPages)));
            return;
        }

        messages.send(player, "warp.list.page-header",
                Messages.of("page", String.valueOf(page)),
                Messages.of("pages", String.valueOf(totalPages)));

        int fromIndex = (page - 1) * LIST_PAGE_SIZE;
        int toIndex = Math.min(filtered.size(), fromIndex + LIST_PAGE_SIZE);
        String lastGroup = null;
        for (Warp warp : filtered.subList(fromIndex, toIndex)) {
            String group = WarpFilter.groupLabel(warp);
            if (!group.equals(lastGroup)) {
                messages.send(player, "warp.list.group-header", Messages.group(group));
                lastGroup = group;
            }
            Component suffix = warp.isEnabled() ? Component.empty() : Component.text(" [disabled]", NamedTextColor.RED);
            messages.send(player, "warp.list.entry", Messages.name(warp.getName()), Placeholder.component("suffix", suffix));
        }

        if (totalPages > 1) {
            messages.send(player, "warp.list.footer");
        }
    }

    private void handleTeleport(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        Optional<Warp> warp = warpService.get(name);
        if (warp.isEmpty()) {
            messages.send(player, "warp.not-found", Messages.name(name));
            return;
        }

        Warp target = warp.get();
        if (!warpService.canAccess(player, target)) {
            messages.send(player, "warp.no-permission", Messages.name(target.getName()));
            return;
        }

        if (target.getWarmupSeconds() > 0) {
            messages.send(player, "warp.warmup.starting", Messages.name(target.getName()),
                    Messages.of("seconds", String.valueOf(target.getWarmupSeconds())));
            warpService.startWarmup(player, target.getWarmupSeconds(), () -> performTeleport(player, target));
        } else {
            performTeleport(player, target);
        }
    }

    private void performTeleport(Player player, Warp target) {
        warpService.teleport(player, target, success -> {
            if (success) {
                messages.send(player, "warp.teleport.success", Messages.name(target.getName()));
            } else {
                messages.send(player, "warp.teleport.failed", Messages.name(target.getName()));
            }
        });
    }
}
