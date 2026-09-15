package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.util.PermissionResolver;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GroupCommand {

    private static final String GROUP_NODE_PREFIX = "group.";

    private final GroupService groupService;
    private final WarpService warpService;
    private final MessageService messages;

    public GroupCommand(GroupService groupService, WarpService warpService, MessageService messages) {
        this.groupService = groupService;
        this.warpService = warpService;
        this.messages = messages;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("group")
                .then(new LiteralArgument("create")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleCreate(player, name, null, null);
                                })
                                .then(new StringArgument("permission")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> permissionSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            String permission = (String) args.getUnchecked("permission");
                                            handleCreate(player, name, permission, null);
                                        })
                                        .then(new GreedyStringArgument("description")
                                                .executesPlayer((player, args) -> {
                                                    String name = (String) args.getUnchecked("name");
                                                    String permission = (String) args.getUnchecked("permission");
                                                    String description = (String) args.getUnchecked("description");
                                                    handleCreate(player, name, permission, description);
                                                })))))
                .then(new LiteralArgument("delete")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleDelete(player, name);
                                })))
                .then(new LiteralArgument("permission")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleUpdatePermission(player, name, null);
                                })
                                .then(new StringArgument("permission")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> permissionSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            String permission = (String) args.getUnchecked("permission");
                                            handleUpdatePermission(player, name, permission);
                                        }))))
                .then(new LiteralArgument("describe")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleUpdateDescription(player, name, null);
                                })
                                .then(new GreedyStringArgument("description")
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            String description = (String) args.getUnchecked("description");
                                            handleUpdateDescription(player, name, description);
                                        }))))
                .then(new LiteralArgument("enable")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSetEnabled(player, name, true);
                                })))
                .then(new LiteralArgument("disable")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> groupNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSetEnabled(player, name, false);
                                })))
                .then(new LiteralArgument("list")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .executesPlayer((player, args) -> {
                            handleList(player);
                        }))
                .register(plugin);
    }

    private String[] groupNames() {
        return groupService.getAll().stream()
                .map(Group::getName)
                .toArray(String[]::new);
    }

    /** "op" plus every currently-configured LuckPerms group name (empty beyond "op" if LuckPerms isn't present). */
    private String[] permissionSuggestions() {
        return Stream.concat(Stream.of(PermissionResolver.OP_ONLY_TOKEN), Permissions.luckPermsGroupNames().stream())
                .toArray(String[]::new);
    }

    private void handleCreate(Player player, String rawName, String permission, String description) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.createGroup(name, permission, description), name,
                "group.create.success", "group.create.failed");
    }

    private void handleDelete(Player player, String rawName) {
        String name = GroupService.normalize(rawName);
        warpService.deleteWarpsInGroup(name, () ->
                groupService.deleteGroup(name).thenAccept(existed -> {
                    if (existed) {
                        messages.send(player, "group.delete.success", Messages.name(name));
                    } else {
                        messages.send(player, "group.not-found", Messages.name(name));
                    }
                }));
    }

    private void handleUpdatePermission(Player player, String rawName, String permission) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.updatePermission(name, permission), name,
                "group.permission.success", "group.update.failed");
    }

    private void handleUpdateDescription(Player player, String rawName, String description) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.updateDescription(name, description), name,
                "group.describe.success", "group.update.failed");
    }

    private void handleSetEnabled(Player player, String rawName, boolean enabled) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.setEnabled(name, enabled), name,
                enabled ? "group.enable.success" : "group.disable.success", "group.update.failed");
    }

    /** Sends the message for {@code successKey} on completion, or the group's validation message (or {@code failureKey}) on failure. */
    private void handleGroupUpdate(Player player, CompletableFuture<Void> operation, String name, String successKey, String failureKey) {
        operation
                .thenRun(() -> messages.send(player, successKey, Messages.name(name)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        messages.send(player, "error.generic", Messages.message(cause.getMessage()));
                    } else {
                        messages.send(player, failureKey, Messages.name(name));
                    }
                    return null;
                });
    }

    private void handleList(Player player) {
        if (groupService.getAll().isEmpty()) {
            messages.send(player, "group.list.empty");
            return;
        }

        String names = groupService.getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .map(this::describeGroup)
                .collect(Collectors.joining("<gray>, </gray>"));
        Messages.send(player, messages.get("group.list.header") + names);
    }

    private String describeGroup(Group group) {
        StringBuilder builder = new StringBuilder(group.getName())
                .append(" <gray>(").append(displayPermission(group.getPermission())).append(")</gray>");
        if (!group.isEnabled()) {
            builder.append(" <red>[disabled]</red>");
        }
        if (group.getDescription() != null) {
            builder.append(" <gray>- ").append(group.getDescription()).append("</gray>");
        }
        return builder.toString();
    }

    /** Shows a LuckPerms-backed permission as just the group name, rather than the raw "group.<name>" node. */
    private static String displayPermission(String permission) {
        if (permission == null) {
            return "public";
        }
        return permission.startsWith(GROUP_NODE_PREFIX) ? permission.substring(GROUP_NODE_PREFIX.length()) : permission;
    }
}
