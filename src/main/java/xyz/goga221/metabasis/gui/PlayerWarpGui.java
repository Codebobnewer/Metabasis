package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Comparator;
import java.util.List;

/** The player-facing warp browser (`/warp gui`) — every warp the viewer can currently access, click to teleport. */
public final class PlayerWarpGui {

    private static final int WIDTH = 9;
    private static final int MAX_ROWS = 6;

    private final WarpService warpService;
    private final MessageService messages;

    public PlayerWarpGui(WarpService warpService, MessageService messages) {
        this.warpService = warpService;
        this.messages = messages;
    }

    public void open(Player player) {
        List<Warp> accessible = warpService.getAll().stream()
                .filter(warp -> warpService.canAccess(player, warp))
                .sorted(Comparator.comparing(Warp::getName))
                .toList();

        int rows = Math.min(MAX_ROWS, Math.max(1, (accessible.size() + WIDTH - 1) / WIDTH));
        Gui gui = Gui.empty(WIDTH, rows);

        int slot = 0;
        for (Warp warp : accessible) {
            if (slot >= WIDTH * rows) {
                break;
            }
            gui.setItem(slot++, new SimpleItem(warpItem(warp), click -> {
                click.getEvent().setCancelled(true);
                click.getPlayer().closeInventory();
                warpService.teleport(click.getPlayer(), warp, success ->
                        messages.send(click.getPlayer(), success ? "warp.teleport.success" : "warp.teleport.failed", Messages.name(warp.getName())));
            }));
        }

        Window window = Window.single(builder -> builder
                .setViewer(player)
                .setTitle("Warps")
                .setGui(gui));
        window.open();
    }

    private static xyz.xenondevs.invui.item.ItemProvider warpItem(Warp warp) {
        Component name = Component.text(warp.getName(), NamedTextColor.GOLD);
        if (warp.getGroupName() != null) {
            return GuiItems.of(Material.ENDER_PEARL, name,
                    Component.text("Group: " + warp.getGroupName(), NamedTextColor.GRAY));
        }
        return GuiItems.of(Material.ENDER_PEARL, name, Component.text("Public", NamedTextColor.GRAY));
    }
}
