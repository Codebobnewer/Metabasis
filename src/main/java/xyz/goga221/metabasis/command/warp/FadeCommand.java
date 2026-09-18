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

/** {@code /warp fade <name> <fadeIn> <stay> <fadeOut>} */
public final class FadeCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("fade")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(
                        new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())),
                        new IntegerArgument("fadeIn", 0),
                        new IntegerArgument("stay", 0),
                        new IntegerArgument("fadeOut", 0))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    int fadeIn = (int) args.getUnchecked("fadeIn");
                    int stay = (int) args.getUnchecked("stay");
                    int fadeOut = (int) args.getUnchecked("fadeOut");
                    handle(player, name, fadeIn, stay, fadeOut);
                });
    }

    private void handle(Player player, String rawName, int fadeIn, int stay, int fadeOut) {
        String name = WarpService.normalize(rawName);
        try {
            Services.getWarpService().setWarpFade(name, fadeIn, stay, fadeOut, saved -> {
                if (saved) {
                    Services.getMessageService().send(player, "warp.fade.success", Messages.name(name));
                } else {
                    Services.getMessageService().send(player, "warp.update.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException e) {
            Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }
}
