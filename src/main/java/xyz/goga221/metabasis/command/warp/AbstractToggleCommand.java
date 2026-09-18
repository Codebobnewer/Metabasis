package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

/** Shared shape for {@code /warp enable} and {@code /warp disable}, which differ only in the literal, the flag, and the success message key. */
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
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .executesPlayer((player, args) -> {
                    String name = WarpService.normalize((String) args.getUnchecked("name"));
                    try {
                        Services.getWarpService().setWarpEnabled(name, enabled, saved -> {
                            if (saved) {
                                Services.getMessageService().send(player, successKey, Messages.name(name));
                            } else {
                                Services.getMessageService().send(player, "warp.update.failed", Messages.name(name));
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        Services.getMessageService().send(player, "error.generic", Messages.message(e.getMessage()));
                    }
                });
    }
}
