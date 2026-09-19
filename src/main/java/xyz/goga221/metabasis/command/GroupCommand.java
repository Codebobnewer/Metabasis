package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.command.group.CreateCommand;
import xyz.goga221.metabasis.command.group.DeleteCommand;
import xyz.goga221.metabasis.command.group.DescribeCommand;
import xyz.goga221.metabasis.command.group.DisableCommand;
import xyz.goga221.metabasis.command.group.EnableCommand;
import xyz.goga221.metabasis.command.group.ListCommand;
import xyz.goga221.metabasis.command.group.PermissionCommand;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Assembles {@code /group} from every leaf command in {@code command.group}. */
public final class GroupCommand {

    public void register(JavaPlugin plugin) {
        new CommandAPICommand("group")
                .withSubcommands(
                        new CreateCommand().getCommand(),
                        new DeleteCommand().getCommand(),
                        new PermissionCommand().getCommand(),
                        new DescribeCommand().getCommand(),
                        new EnableCommand().getCommand(),
                        new DisableCommand().getCommand(),
                        new ListCommand().getCommand())
                .register(plugin);
    }
}
