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

/** {@code /group create <name> [permission] [description]} */
public final class CreateCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("create")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name"))
                .withOptionalArguments(
                        new StringArgument("permission").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.permissionSuggestions())),
                        new GreedyStringArgument("description"))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    String permission = (String) args.getOptional("permission").orElse(null);
                    String description = (String) args.getOptional("description").orElse(null);
                    handle(player, name, permission, description);
                });
    }

    private void handle(Player player, String rawName, String permission, String description) {
        String name = GroupService.normalize(rawName);
        GroupCommandSupport.handleUpdate(player, Services.getGroupService().createGroup(name, permission, description), name,
                "group.create.success", "group.create.failed");
    }
}
