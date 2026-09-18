package xyz.goga221.metabasis.command;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Assembles {@code /group} from every leaf command found in {@code command.group}. */
public final class GroupCommand {

    private static final String LEAF_PACKAGE = "xyz.goga221.metabasis.command.group";

    public void register(JavaPlugin plugin) {
        new CommandAPICommand("group")
                .withSubcommands(CommandGroupScanner.scan(LEAF_PACKAGE).toArray(new CommandAPICommand[0]))
                .register(plugin);
    }
}
