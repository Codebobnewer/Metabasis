package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.spawn.SpawnService;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.util.PermissionResolver;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SpawnCommand {

    private final SpawnService spawnService;
    private final MessageService messages;

    public SpawnCommand(SpawnService spawnService, MessageService messages) {
        this.spawnService = spawnService;
        this.messages = messages;
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
                .then(new LiteralArgument("permission")
                        .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                        .then(new StringArgument("world")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> worldNames()))
                                .executesPlayer((player, args) -> {
                                    String world = (String) args.getUnchecked("world");
                                    handleUpdatePermission(player, world, null);
                                })
                                .then(new StringArgument("permission")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> permissionSuggestions()))
                                        .executesPlayer((player, args) -> {
                                            String world = (String) args.getUnchecked("world");
                                            String permission = (String) args.getUnchecked("permission");
                                            handleUpdatePermission(player, world, permission);
                                        }))))
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

    /** "op" plus every currently-configured LuckPerms group name (empty beyond "op" if LuckPerms isn't present). */
    private String[] permissionSuggestions() {
        return Stream.concat(Stream.of(PermissionResolver.OP_ONLY_TOKEN), Permissions.luckPermsGroupNames().stream())
                .toArray(String[]::new);
    }

    private void handleSet(Player player, String rawWorld) {
        spawnService.resolveWorld(rawWorld).ifPresentOrElse(
                world -> spawnService.setSpawn(world, player.getLocation(), player.getUniqueId())
                        .thenRun(() -> messages.send(player, "spawn.set.success", Messages.name(world.getName())))
                        .exceptionally(throwable -> {
                            messages.send(player, "spawn.set.failed", Messages.name(rawWorld));
                            return null;
                        }),
                () -> messages.send(player, "spawn.world-not-found", Messages.name(rawWorld))
        );
    }

    private void handleUpdatePermission(Player player, String rawWorld, String permission) {
        spawnService.resolveWorld(rawWorld).ifPresentOrElse(
                world -> handleUpdatePermission(player, world.getName(), rawWorld, permission),
                () -> messages.send(player, "spawn.world-not-found", Messages.name(rawWorld))
        );
    }

    private void handleUpdatePermission(Player player, String exactWorldName, String rawWorld, String permission) {
        CompletableFuture<Void> operation = spawnService.updatePermission(exactWorldName, permission);
        operation
                .thenRun(() -> messages.send(player, "spawn.permission.success", Messages.name(exactWorldName)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        messages.send(player, "error.generic", Messages.message(cause.getMessage()));
                    } else {
                        messages.send(player, "spawn.permission.failed", Messages.name(rawWorld));
                    }
                    return null;
                });
    }

    private void handleTeleport(Player player, String rawWorld) {
        spawnService.teleportToWorldSpawn(player, rawWorld, outcome -> {
            switch (outcome) {
                case TELEPORTED_CUSTOM -> messages.send(player, "spawn.teleport.custom", Messages.name(rawWorld));
                case TELEPORTED_DEFAULT -> messages.send(player, "spawn.teleport.default", Messages.name(rawWorld));
                case WORLD_NOT_FOUND -> messages.send(player, "spawn.world-not-found", Messages.name(rawWorld));
                case NO_PERMISSION -> messages.send(player, "spawn.no-permission", Messages.name(rawWorld));
                case TELEPORT_FAILED -> messages.send(player, "spawn.teleport.failed", Messages.name(rawWorld));
            }
        });
    }
}
