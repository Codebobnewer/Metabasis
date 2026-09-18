package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /warp create <name>}. Only ever creates a brand-new warp — refuses if the name is already taken, unlike {@link MoveCommand}. */
public final class CreateCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("create")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name"))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    handle(player, name);
                });
    }

    private void handle(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        if (Services.getWarpService().get(name).isPresent()) {
            Services.getMessageService().send(player, "warp.create.exists", Messages.name(name));
            return;
        }

        try {
            Services.getWarpService().createWarp(rawName, player.getLocation(), player.getUniqueId(), saved -> {
                if (saved) {
                    Services.getMessageService().send(player, "warp.create.success", Messages.name(name));
                } else {
                    Services.getMessageService().send(player, "warp.create.failed", Messages.name(name));
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }
}
