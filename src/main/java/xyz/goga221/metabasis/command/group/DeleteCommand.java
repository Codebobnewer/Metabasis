package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /group delete <name>} */
public final class DeleteCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("delete")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.groupNames())))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    handle(player, name);
                });
    }

    private void handle(Player player, String rawName) {
        String name = GroupService.normalize(rawName);
        Services.getWarpService().deleteWarpsInGroup(name, () ->
                Services.getGroupService().deleteGroup(name).thenAccept(existed -> {
                    if (existed) {
                        Services.getMessageService().send(player, "group.delete.success", Messages.name(name));
                    } else {
                        Services.getMessageService().send(player, "group.not-found", Messages.name(name));
                    }
                }));
    }
}
