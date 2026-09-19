package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.command.BaseCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;

/**
 * {@code /warp list [page]}. Registered separately from {@link ListGroupCommand} (both are attached
 * to {@code warp} under the name {@code list}) rather than as one node with its own optional argument
 * plus a nested {@code group} subcommand: CommandAPI's flatten() mutates the shared argument list it
 * passes to child subcommands, so a node's own optional args leak into the subcommand's required ones
 * and register() throws OptionalArgumentException.
 */
public final class ListCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("list")
                .withOptionalArguments(new IntegerArgument("page", 1))
                .executesPlayer((player, args) -> {
                    int page = (int) args.getOptional("page").orElse(1);
                    WarpCommandSupport.renderList(player, null, page);
                });
    }
}
