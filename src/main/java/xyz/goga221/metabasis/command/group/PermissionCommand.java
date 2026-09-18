package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /group permission <name> [permission]} */
public final class PermissionCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("permission")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.groupNames())))
                .withOptionalArguments(new StringArgument("permission").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.permissionSuggestions())))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    String permission = (String) args.getOptional("permission").orElse(null);
                    handle(player, name, permission);
                });
    }

    private void handle(Player player, String rawName, String permission) {
        String name = GroupService.normalize(rawName);
        GroupCommandSupport.handleUpdate(player, Services.getGroupService().updatePermission(name, permission), name,
                "group.permission.success", "group.update.failed");
    }
}
