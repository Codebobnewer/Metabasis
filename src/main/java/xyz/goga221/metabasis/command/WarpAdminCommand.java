package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.warp.CreateCommand;
import xyz.goga221.metabasis.command.warp.DeleteCommand;
import xyz.goga221.metabasis.command.warp.DisableCommand;
import xyz.goga221.metabasis.command.warp.EnableCommand;
import xyz.goga221.metabasis.command.warp.FadeCommand;
import xyz.goga221.metabasis.command.warp.GroupAssignCommand;
import xyz.goga221.metabasis.command.warp.HistoryCommand;
import xyz.goga221.metabasis.command.warp.MassportCommand;
import xyz.goga221.metabasis.command.warp.MoveCommand;
import xyz.goga221.metabasis.command.warp.WarmupCommand;
import xyz.goga221.metabasis.gui.AdminMenuGui;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Assembles {@code /warpadmin}: every admin-only warp action, kept out of {@code /warp} so that
 * command's tab-complete only ever offers warp names and player-facing subcommands, never mixed
 * with admin literals like {@code create}/{@code del}/{@code massport}.
 */
public final class WarpAdminCommand {

    public void register(JavaPlugin plugin) {
        new CommandAPICommand("warpadmin")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .executesPlayer((player, args) -> {
                    new AdminMenuGui(Services.getGuiContext()).open(player);
                })
                .withSubcommands(
                        new CreateCommand().getCommand(),
                        new MoveCommand().getCommand(),
                        new DeleteCommand().getCommand(),
                        new GroupAssignCommand().getCommand(),
                        new EnableCommand().getCommand(),
                        new DisableCommand().getCommand(),
                        new FadeCommand().getCommand(),
                        new WarmupCommand().getCommand(),
                        new HistoryCommand().getCommand(),
                        new MassportCommand().getCommand())
                .register(plugin);
    }
}
