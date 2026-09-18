package xyz.goga221.metabasis.command.spawn;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /spawn set <world>} */
public final class SetCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("set")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("world").replaceSuggestions(ArgumentSuggestions.strings(info -> SpawnCommandSupport.worldNames())))
                .executesPlayer((player, args) -> {
                    String world = (String) args.getUnchecked("world");
                    handle(player, world);
                });
    }

    private void handle(Player player, String rawWorld) {
        Services.getSpawnService().resolveWorld(rawWorld).ifPresentOrElse(
                world -> Services.getSpawnService().setSpawn(world, player.getLocation(), player.getUniqueId())
                        .thenRun(() -> Services.getMessageService().send(player, "spawn.set.success", Messages.name(world.getName())))
                        .exceptionally(throwable -> {
                            Services.getMessageService().send(player, "spawn.set.failed", Messages.name(rawWorld));
                            return null;
                        }),
                () -> Services.getMessageService().send(player, "spawn.world-not-found", Messages.name(rawWorld))
        );
    }
}
