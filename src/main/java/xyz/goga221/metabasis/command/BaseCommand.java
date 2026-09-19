package xyz.goga221.metabasis.command;

import dev.jorel.commandapi.CommandAPICommand;

/** One leaf subcommand, constructed with a no-arg constructor and attached by its root command via {@link #getCommand()}. */
public abstract class BaseCommand {

    public abstract CommandAPICommand register();

    public CommandAPICommand getCommand() {
        return register();
    }
}
