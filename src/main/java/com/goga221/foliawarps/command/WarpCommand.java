package com.goga221.foliawarps.command;

import com.goga221.foliawarps.util.Messages;
import com.goga221.foliawarps.warp.Warp;
import com.goga221.foliawarps.warp.WarpService;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Collectors;

public final class WarpCommand {

    private static final String ADMIN_PERMISSION = "foliawarps.admin";

    private final WarpService warpService;

    public WarpCommand(WarpService warpService) {
        this.warpService = warpService;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("warp")
                .then(new LiteralArgument("set")
                        .withPermission(ADMIN_PERMISSION)
                        .then(new StringArgument("name")
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleSet(player, name);
                                })))
                .then(new LiteralArgument("del")
                        .withPermission(ADMIN_PERMISSION)
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> warpNames()))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.getUnchecked("name");
                                    handleDelete(player, name);
                                })))
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

    private void handleSet(Player player, String rawName) {
        String name = WarpService.normalize(rawName);

        warpService.createWarp(name, player.getLocation(), player.getUniqueId())
                .thenRun(() -> Messages.send(player, "<green>Warp <white><name></white> created.</green>", Messages.name(name)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        Messages.send(player, "<red>" + cause.getMessage() + "</red>");
                    } else {
                        Messages.send(player, "<red>Failed to save warp <white><name></white>.</red>", Messages.name(name));
                    }
                    return null;
                });
    }

    private void handleDelete(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        warpService.deleteWarp(name).thenAccept(deleted -> {
            if (deleted) {
                Messages.send(player, "<green>Warp <white><name></white> deleted.</green>", Messages.name(name));
            } else {
                Messages.send(player, "<red>No warp named <white><name></white> exists.</red>", Messages.name(name));
            }
        });
    }

    private void handleList(Player player) {
        if (warpService.getAll().isEmpty()) {
            Messages.send(player, "<yellow>There are no warps yet.</yellow>");
            return;
        }

        String names = warpService.getAll().stream()
                .map(Warp::getName)
                .sorted()
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
        warpService.teleport(player, target, success -> {
            if (success) {
                Messages.send(player, "<green>Teleported to <white><name></white>.</green>", Messages.name(target.getName()));
            } else {
                Messages.send(player, "<red>Teleportation to <white><name></white> failed. The world may not be loaded.</red>", Messages.name(target.getName()));
            }
        });
    }
}
