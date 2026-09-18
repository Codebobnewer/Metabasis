package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

/** Shared shape for {@code /group enable} and {@code /group disable}, which differ only in the literal, the flag, and the success message key. */
abstract class AbstractToggleCommand extends BaseCommand {

    private final String literal;
    private final boolean enabled;
    private final String successKey;

    protected AbstractToggleCommand(String literal, boolean enabled, String successKey) {
        this.literal = literal;
        this.enabled = enabled;
        this.successKey = successKey;
    }

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand(literal)
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> GroupCommandSupport.groupNames())))
                .executesPlayer((player, args) -> {
                    String name = GroupService.normalize((String) args.getUnchecked("name"));
                    GroupCommandSupport.handleUpdate(player, Services.getGroupService().setEnabled(name, enabled), name,
                            successKey, "group.update.failed");
                });
    }
}
