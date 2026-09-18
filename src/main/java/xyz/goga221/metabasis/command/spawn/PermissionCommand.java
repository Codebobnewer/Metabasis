package xyz.goga221.metabasis.command.spawn;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/** {@code /spawn permission <world> [permission]} */
public final class PermissionCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("permission")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("world").replaceSuggestions(ArgumentSuggestions.strings(info -> SpawnCommandSupport.worldNames())))
                .withOptionalArguments(new StringArgument("permission").replaceSuggestions(ArgumentSuggestions.strings(info -> SpawnCommandSupport.permissionSuggestions())))
                .executesPlayer((player, args) -> {
                    String world = (String) args.getUnchecked("world");
                    String permission = (String) args.getOptional("permission").orElse(null);
                    handle(player, world, permission);
                });
    }

    private void handle(Player player, String rawWorld, String permission) {
        Services.getSpawnService().resolveWorld(rawWorld).ifPresentOrElse(
                world -> handle(player, world.getName(), rawWorld, permission),
                () -> Services.getMessageService().send(player, "spawn.world-not-found", Messages.name(rawWorld))
        );
    }

    private void handle(Player player, String exactWorldName, String rawWorld, String permission) {
        CompletableFuture<Void> operation = Services.getSpawnService().updatePermission(exactWorldName, permission);
        operation
                .thenRun(() -> Services.getMessageService().send(player, "spawn.permission.success", Messages.name(exactWorldName)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        Services.getMessageService().send(player, "error.generic", Messages.message(cause.getMessage()));
                    } else {
                        Services.getMessageService().send(player, "spawn.permission.failed", Messages.name(rawWorld));
                    }
                    return null;
                });
    }
}
