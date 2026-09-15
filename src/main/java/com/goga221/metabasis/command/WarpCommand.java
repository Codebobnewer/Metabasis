package com.goga221.metabasis.command;

import com.goga221.metabasis.group.Group;
import com.goga221.metabasis.group.GroupService;
import com.goga221.metabasis.util.Messages;
import com.goga221.metabasis.util.Permissions;
import com.goga221.metabasis.warp.Warp;
import com.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WarpCommand {

    private static final String NONE_GROUP_TOKEN = "none";

    private final WarpService warpService;
    private final GroupService groupService;

    public WarpCommand(WarpService warpService, GroupService groupService) {
        this.warpService = warpService;
        this.groupService = groupService;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("warp")
                .then(new LiteralArgument("set")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("name")
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSet(player, name);
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
                .then(new LiteralArgument("list")
                        .executesPlayer((player, args) -> {
                            handleList(player);
                        }))
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

    private void handleSet(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        try {
            warpService.createWarp(rawName, player.getLocation(), player.getUniqueId(), saved -> {
                if (saved) {
                    Messages.send(player, "<green>Warp <white><name></white> created.</green>", Messages.name(name));
                } else {
                    Messages.send(player, "<red>Failed to save warp <white><name></white>.</red>", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            Messages.send(player, "<red>" + e.getMessage() + "</red>");
        }
    }

    private void handleDelete(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        warpService.deleteWarp(name, deleted -> {
            if (deleted) {
                Messages.send(player, "<green>Warp <white><name></white> deleted.</green>", Messages.name(name));
            } else {
                Messages.send(player, "<red>No warp named <white><name></white> exists.</red>", Messages.name(name));
            }
        });
    }

    private void handleGroupAssign(Player player, String rawWarpName, String rawGroupName) {
        String warpName = WarpService.normalize(rawWarpName);
        String groupToken = GroupService.normalize(rawGroupName);
        String groupName = NONE_GROUP_TOKEN.equals(groupToken) ? null : groupToken;

        if (groupName != null && !groupService.exists(groupName)) {
            Messages.send(player, "<red>No group named <white><name></white> exists.</red>", Messages.name(groupName));
            return;
        }

        try {
            warpService.setWarpGroup(warpName, groupName, saved -> {
                if (saved) {
                    if (groupName == null) {
                        Messages.send(player, "<green>Warp <white><name></white> is now public.</green>", Messages.name(warpName));
                    } else {
                        Messages.send(player, "<green>Warp <white><name></white> assigned to group <white><group></white>.</green>",
                                Messages.name(warpName), Messages.group(groupName));
                    }
                } else {
                    Messages.send(player, "<red>Failed to update warp <white><name></white>.</red>", Messages.name(warpName));
                }
            });
        } catch (IllegalArgumentException e) {
            Messages.send(player, "<red>" + e.getMessage() + "</red>");
        }
    }

    private void handleList(Player player) {
        if (warpService.getAll().isEmpty()) {
            Messages.send(player, "<yellow>There are no warps yet.</yellow>");
            return;
        }

        String names = warpService.getAll().stream()
                .sorted(Comparator.comparing(Warp::getName))
                .map(warp -> warp.getGroupName() == null
                        ? warp.getName()
                        : warp.getName() + " <gray>(" + warp.getGroupName() + ")</gray>")
                .collect(Collectors.joining("<gray>, </gray>"));
        Messages.send(player, "<gold>Warps:</gold> " + names);
    }

    private void handleTeleport(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        Optional<Warp> warp = warpService.get(name);
        if (warp.isEmpty()) {
            Messages.send(player, "<red>No warp named <white><name></white> exists.</red>", Messages.name(name));
            return;
        }

        Warp target = warp.get();
        if (!warpService.canAccess(player, target)) {
            Messages.send(player, "<red>You don't have permission to warp to <white><name></white>.</red>", Messages.name(target.getName()));
            return;
        }

        warpService.teleport(player, target, success -> {
            if (success) {
                Messages.send(player, "<green>Teleported to <white><name></white>.</green>", Messages.name(target.getName()));
            } else {
                Messages.send(player, "<red>Teleportation to <white><name></white> failed. The world may not be loaded.</red>", Messages.name(target.getName()));
            }
        });
    }
}
