package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /warp warmup <name> <seconds>} */
public final class WarmupCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("warmup")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(
                        new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())),
                        new IntegerArgument("seconds", 0))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    int seconds = (int) args.getUnchecked("seconds");
                    handle(player, name, seconds);
                });
    }

    private void handle(Player player, String rawName, int seconds) {
        String name = WarpService.normalize(rawName);
        try {
            Services.getWarpService().setWarpWarmup(name, seconds, saved -> {
                if (saved) {
                    Services.getMessageService().send(player, "warp.warmup.set.success", Messages.name(name), Messages.of("seconds", String.valueOf(seconds)));
                } else {
                    Services.getMessageService().send(player, "warp.update.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException e) {
            Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }
}
