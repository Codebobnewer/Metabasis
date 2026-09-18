package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.stream.Collectors;

/** {@code /group list} */
public final class ListCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("list")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .executesPlayer((player, args) -> {
                    handle(player);
                });
    }

    private void handle(Player player) {
        if (Services.getGroupService().getAll().isEmpty()) {
            Services.getMessageService().send(player, "group.list.empty");
            return;
        }

        String names = Services.getGroupService().getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .map(this::describeGroup)
                .collect(Collectors.joining("<gray>, </gray>"));
        Messages.send(player, Services.getMessageService().get("group.list.header") + names);
    }

    private String describeGroup(Group group) {
        StringBuilder builder = new StringBuilder(group.getName())
                .append(" <gray>(").append(displayPermission(group.getPermission())).append(")</gray>");
        if (!group.isEnabled()) {
            builder.append(" <red>[disabled]</red>");
        }
        if (group.getDescription() != null) {
            builder.append(" <gray>- ").append(group.getDescription()).append("</gray>");
        }
        return builder.toString();
    }

    private static String displayPermission(String permission) {
        return permission == null ? "public" : permission;
    }
}
