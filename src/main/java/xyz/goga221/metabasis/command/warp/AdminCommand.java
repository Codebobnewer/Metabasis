package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.gui.AdminMenuGui;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;

/** {@code /warp admin} */
public final class AdminCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("admin")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .executesPlayer((player, args) -> {
                    new AdminMenuGui(Services.getGuiContext()).open(player);
                });
    }
}
