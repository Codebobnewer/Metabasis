package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /group describe <name> [description]} */
public final class DescribeCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("describe")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.groupNames())))
                .withOptionalArguments(new GreedyStringArgument("description"))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    String description = (String) args.getOptional("description").orElse(null);
                    handle(player, name, description);
                });
    }

    private void handle(Player player, String rawName, String description) {
        String name = GroupService.normalize(rawName);
        GroupCommandSupport.handleUpdate(player, Services.getGroupService().updateDescription(name, description), name,
                "group.describe.success", "group.update.failed");
    }
}
