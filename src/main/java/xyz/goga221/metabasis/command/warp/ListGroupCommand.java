package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.command.BaseCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

/** {@code /warp list group <groupName> [page]}. Kept out of {@link ListCommand} — see its class comment for why. */
public final class ListGroupCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("list")
                .withSubcommand(new CommandAPICommand("group")
                        .withArguments(new StringArgument("groupName").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.listGroupSuggestions())))
                        .withOptionalArguments(new IntegerArgument("page", 1))
                        .executesPlayer((player, args) -> {
                            String groupName = (String) args.getUnchecked("groupName");
                            int page = (int) args.getOptional("page").orElse(1);
                            WarpCommandSupport.renderList(player, groupName, page);
                        }));
    }
}
