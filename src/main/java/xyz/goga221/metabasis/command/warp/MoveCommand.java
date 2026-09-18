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

/** {@code /warp move <name>}. Only ever relocates an existing warp — refuses if the name doesn't exist yet, unlike {@link CreateCommand}. */
public final class MoveCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("move")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    handle(player, name);
                });
    }

    private void handle(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        if (Services.getWarpService().get(name).isEmpty()) {
            Services.getMessageService().send(player, "warp.not-found", Messages.name(name));
            return;
        }

        try {
            Services.getWarpService().createWarp(rawName, player.getLocation(), player.getUniqueId(), saved -> {
                if (saved) {
                    Services.getMessageService().send(player, "warp.move.success", Messages.name(name));
                } else {
                    Services.getMessageService().send(player, "warp.move.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }
}
