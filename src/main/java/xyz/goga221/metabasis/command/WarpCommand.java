package xyz.goga221.metabasis.command;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.command.warp.GuiCommand;
import xyz.goga221.metabasis.command.warp.ListCommand;
import xyz.goga221.metabasis.command.warp.ListGroupCommand;
import xyz.goga221.metabasis.command.warp.WarpCommandSupport;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Assembles {@code /warp}: teleport-by-name is the root's own behavior, {@code list}/{@code gui} are
 * the only leaf commands here. Admin-only actions (create, del, massport, ...) live under
 * {@link WarpAdminCommand} instead, so this command's tab-complete only ever shows warp names and
 * player-facing subcommands.
 */
public final class WarpCommand {

    public void register(JavaPlugin plugin) {
        new CommandAPICommand("warp")
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> WarpCommandSupport.warpNames())))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getUnchecked("name");
                    handleTeleport(player, name);
                })
                .withSubcommands(
                        new GuiCommand().getCommand(),
                        new ListCommand().getCommand(),
                        new ListGroupCommand().getCommand())
                .register(plugin);
    }

    private void handleTeleport(Player player, String rawName) {
        String name = WarpService.normalize(rawName);
        Optional<Warp> warp = Services.getWarpService().get(name);
        if (warp.isEmpty()) {
            Services.getMessageService().send(player, "warp.not-found", Messages.name(name));
            return;
        }

        Warp target = warp.get();
        if (!Services.getWarpService().canAccess(player, target)) {
            Services.getMessageService().send(player, "warp.no-permission", Messages.name(target.getName()));
            return;
        }

        if (target.getWarmupSeconds() > 0) {
            Services.getMessageService().send(player, "warp.warmup.starting", Messages.name(target.getName()),
                    Messages.of("seconds", String.valueOf(target.getWarmupSeconds())));
            Services.getWarpService().startWarmup(player, target.getWarmupSeconds(), () -> performTeleport(player, target));
        } else {
            performTeleport(player, target);
        }
    }

    private void performTeleport(Player player, Warp target) {
        Services.getWarpService().teleport(player, target, success -> {
            if (success) {
                Services.getMessageService().send(player, "warp.teleport.success", Messages.name(target.getName()));
            } else {
                Services.getMessageService().send(player, "warp.teleport.failed", Messages.name(target.getName()));
            }
        });
    }
}
