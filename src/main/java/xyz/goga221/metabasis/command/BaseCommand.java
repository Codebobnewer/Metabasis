package xyz.goga221.metabasis.command;

import dev.jorel.commandapi.CommandAPICommand;

/** One leaf subcommand, discovered by {@link CommandGroupScanner} and instantiated via its no-arg constructor. */
public abstract class BaseCommand {

    public abstract CommandAPICommand register();

    public CommandAPICommand getCommand() {
        return register();
    }
}
