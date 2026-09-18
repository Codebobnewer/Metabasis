package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.gui.PlayerWarpGui;
import dev.jorel.commandapi.CommandAPICommand;

/** {@code /warp gui} */
public final class GuiCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("gui")
                .executesPlayer((player, args) -> {
                    new PlayerWarpGui(Services.getWarpService(), Services.getMessageService()).open(player);
                });
    }
}
