package xyz.goga221.metabasis.command.warp;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.BaseCommand;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/** {@code /warp massport <name> [override]} */
public final class MassportCommand extends BaseCommand {

    @Override
    public CommandAPICommand register() {
        return new CommandAPICommand("massport")
                .withRequirement(sender -> Permissions.check(sender, Permissions.ADMIN))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .withOptionalArguments(new LiteralArgument(WarpCommandSupport.OVERRIDE_TOKEN))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    boolean override = args.getOptional(WarpCommandSupport.OVERRIDE_TOKEN).isPresent();
                    handle(player, name, override);
                });
    }

    private void handle(Player sender, String rawName, boolean override) {
        String name = WarpService.normalize(rawName);
        Optional<Warp> warp = Services.getWarpService().get(name);
        if (warp.isEmpty()) {
            Services.getMessageService().send(sender, "warp.not-found", Messages.name(name));
            return;
        }

        Warp target = warp.get();
        int total = 0;
        int skipped = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            total++;
            if (!override && !Services.getWarpService().canAccess(player, target)) {
                skipped++;
                continue;
            }
            Services.getWarpService().teleport(player, target, success -> {
            });
        }

        int teleported = total - skipped;
        Services.getMessageService().send(sender, "warp.massport.summary",
                Messages.name(name),
                Messages.of("teleported", String.valueOf(teleported)),
                Messages.of("total", String.valueOf(total)),
                Messages.of("skipped", String.valueOf(skipped)));
    }
}
