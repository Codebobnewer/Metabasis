package com.goga221.metabasis.command;

import com.goga221.metabasis.group.Group;
import com.goga221.metabasis.group.GroupService;
import com.goga221.metabasis.util.Messages;
import com.goga221.metabasis.util.Permissions;
import com.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
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

    public GroupCommand(GroupService groupService, WarpService warpService) {
        this.groupService = groupService;
        this.warpService = warpService;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("group")
                .then(new LiteralArgument("create")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleCreate(player, name, null);
                                })
                                .then(new StringArgument("permission")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> permissionSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String name = (String) args.getUnchecked("name");
                                            String permission = (String) args.getUnchecked("permission");
                                            handleCreate(player, name, permission);
                                        }))))
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
        return Stream.concat(Stream.of(GroupService.OP_ONLY_TOKEN), Permissions.luckPermsGroupNames().stream())
                .toArray(String[]::new);
    }

    private void handleCreate(Player player, String rawName, String permission) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.createGroup(name, permission), name,
                "<green>Group <white><name></white> created.</green>",
                "<red>Failed to create group <white><name></white>.</red>");
    }

    private void handleDelete(Player player, String rawName) {
        String name = GroupService.normalize(rawName);
        warpService.unassignWarpsInGroup(name, () ->
                groupService.deleteGroup(name).thenAccept(existed -> {
                    if (existed) {
                        Messages.send(player, "<green>Group <white><name></white> deleted; any assigned warps were unassigned.</green>", Messages.name(name));
                    } else {
                        Messages.send(player, "<red>No group named <white><name></white> exists.</red>", Messages.name(name));
                    }
                }));
    }

    private void handleUpdatePermission(Player player, String rawName, String permission) {
        String name = GroupService.normalize(rawName);
        handleGroupUpdate(player, groupService.updatePermission(name, permission), name,
                "<green>Group <white><name></white> permission updated.</green>",
                "<red>Failed to update group <white><name></white>.</red>");
    }

    /** Sends {@code successMessage} on completion, or the group's validation message (or {@code genericFailureMessage}) on failure. */
    private void handleGroupUpdate(Player player, CompletableFuture<Void> operation, String name, String successMessage, String genericFailureMessage) {
        operation
                .thenRun(() -> Messages.send(player, successMessage, Messages.name(name)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        Messages.send(player, "<red>" + cause.getMessage() + "</red>");
                    } else {
                        Messages.send(player, genericFailureMessage, Messages.name(name));
                    }
                    return null;
                });
    }

    private void handleList(Player player) {
        if (groupService.getAll().isEmpty()) {
            Messages.send(player, "<yellow>There are no groups yet.</yellow>");
            return;
        }

        String names = groupService.getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .map(group -> group.getName() + " <gray>(" + displayPermission(group.getPermission()) + ")</gray>")
                .collect(Collectors.joining("<gray>, </gray>"));
        Messages.send(player, "<gold>Groups:</gold> " + names);
    }

    /** Shows a LuckPerms-backed permission as just the group name, rather than the raw "group.<name>" node. */
    private static String displayPermission(String permission) {
        if (permission == null) {
            return "public";
        }
        return permission.startsWith(GROUP_NODE_PREFIX) ? permission.substring(GROUP_NODE_PREFIX.length()) : permission;
    }
}
