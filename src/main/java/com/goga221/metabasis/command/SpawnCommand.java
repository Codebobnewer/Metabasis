package com.goga221.metabasis.command;

import com.goga221.metabasis.spawn.SpawnService;
import com.goga221.metabasis.util.Messages;
import com.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnCommand {

    private final SpawnService spawnService;

    public SpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("spawn")
                .executesPlayer((player, args) -> {
                    handleTeleport(player, player.getWorld().getName());
                })
                .then(new LiteralArgument("set")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("world")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> worldNames()))
                                .executesPlayer((player, args) -> {
                                    String world = (String) args.getUnchecked("world");
                                    handleSet(player, world);
                                })))
                .then(new StringArgument("world")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> worldNames()))
                        .executesPlayer((player, args) -> {
                            String world = (String) args.getUnchecked("world");
                            handleTeleport(player, world);
                        }))
                .register(plugin);
    }

    private String[] worldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .toArray(String[]::new);
    }

    private void handleSet(Player player, String rawWorld) {
        spawnService.resolveWorld(rawWorld).ifPresentOrElse(
                world -> spawnService.setSpawn(world, player.getLocation(), player.getUniqueId())
                        .thenRun(() -> Messages.send(player, "<green>Spawn for <white><name></white> set to your current location.</green>", Messages.name(world.getName())))
                        .exceptionally(throwable -> {
                            Messages.send(player, "<red>Failed to save spawn for <white><name></white>.</red>", Messages.name(rawWorld));
                            return null;
                        }),
                () -> Messages.send(player, "<red>No world named <white><name></white> is loaded.</red>", Messages.name(rawWorld))
        );
    }

    private void handleTeleport(Player player, String rawWorld) {
        spawnService.teleportToWorldSpawn(player, rawWorld, outcome -> {
            switch (outcome) {
                case TELEPORTED_CUSTOM ->
                        Messages.send(player, "<green>Teleported to <white><name></white>'s spawn.</green>", Messages.name(rawWorld));
                case TELEPORTED_DEFAULT ->
                        Messages.send(player, "<green>Teleported to <white><name></white>'s default spawn (no custom spawn set).</green>", Messages.name(rawWorld));
                case WORLD_NOT_FOUND ->
                        Messages.send(player, "<red>No world named <white><name></white> is loaded.</red>", Messages.name(rawWorld));
                case TELEPORT_FAILED ->
                        Messages.send(player, "<red>Teleportation to <white><name></white>'s spawn failed.</red>", Messages.name(rawWorld));
            }
        });
    }
}
