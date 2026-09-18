package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

/** {@code /warp group <warpName> <groupName>} */
public final class GroupAssignCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("group")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(
                        new StringArgument("warpName").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())),
                        new StringArgument("groupName").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.groupNameSuggestions())))
                .executesPlayer((player, args) -> {
                    String warpName = (String) args.getUnchecked("warpName");
                    String groupName = (String) args.getUnchecked("groupName");
                    handle(player, warpName, groupName);
                });
    }

    private void handle(Player player, String rawWarpName, String rawGroupName) {
        String warpName = WarpService.normalize(rawWarpName);
        String groupToken = GroupService.normalize(rawGroupName);
        String groupName = WarpCommandSupport.NONE_GROUP_TOKEN.equals(groupToken) ? null : groupToken;

        if (groupName != null && !Services.getGroupService().exists(groupName)) {
            Services.getMessageService().send(player, "group.not-found", Messages.name(groupName));
            return;
        }

        try {
            Services.getWarpService().setWarpGroup(warpName, groupName, saved -> {
                if (saved) {
                    if (groupName == null) {
                        Services.getMessageService().send(player, "warp.group.cleared", Messages.name(warpName));
                    } else {
                        Services.getMessageService().send(player, "warp.group.assigned", Messages.name(warpName), Messages.group(groupName));
                    }
                } else {
                    Services.getMessageService().send(player, "warp.update.failed", Messages.name(warpName));
                }
            });
        } catch (IllegalArgumentException e) {
            Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
        }
    }
}
