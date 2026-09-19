package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.spawn.PermissionCommand;
import xyz.goga221.metabasis.command.spawn.SetCommand;
import xyz.goga221.metabasis.command.spawn.SpawnCommandSupport;
import xyz.goga221.metabasis.util.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Assembles {@code /spawn}: teleport-to-world is the root's own behavior, every other branch is a leaf command in {@code command.spawn}. */
public final class SpawnCommand {

    public void register(JavaPlugin plugin) {
        new CommandAPICommand("spawn")
                .withOptionalArguments(new StringArgument("world").replaceSuggestions(ArgumentSuggestions.strings(info -> SpawnCommandSupport.worldNames())))
                .executesPlayer((player, args) -> {
                    String world = (String) args.getOptional("world").orElse(player.getWorld().getName());
                    handleTeleport(player, world);
                })
                .withSubcommands(
                        new SetCommand().getCommand(),
                        new PermissionCommand().getCommand())
                .register(plugin);
    }

    private void handleTeleport(Player player, String rawWorld) {
        Services.getSpawnService().teleportToWorldSpawn(player, rawWorld, outcome -> {
            switch (outcome) {
                case TELEPORTED_CUSTOM -> Services.getMessageService().send(player, "spawn.teleport.custom", Messages.name(rawWorld));
                case TELEPORTED_DEFAULT -> Services.getMessageService().send(player, "spawn.teleport.default", Messages.name(rawWorld));
                case WORLD_NOT_FOUND -> Services.getMessageService().send(player, "spawn.world-not-found", Messages.name(rawWorld));
                case NO_PERMISSION -> Services.getMessageService().send(player, "spawn.no-permission", Messages.name(rawWorld));
                case TELEPORT_FAILED -> Services.getMessageService().send(player, "spawn.teleport.failed", Messages.name(rawWorld));
            }
        });
    }
}
