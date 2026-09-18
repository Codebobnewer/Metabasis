package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /warp del <name>} */
public final class DeleteCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("del")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    handle(player, name);
                });
    }

    private void handle(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        Services.getWarpService().deleteWarp(name, deleted -> {
            if (deleted) {
                Services.getMessageService().send(player, "warp.delete.success", Messages.name(name));
            } else {
                Services.getMessageService().send(player, "warp.not-found", Messages.name(name));
            }
        });
    }
}
