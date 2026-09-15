package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.warp.Warp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Comparator;
import java.util.List;

/** Every warp, click to open its detail/edit screen. */
public final class AdminWarpListGui {

    private static final int WIDTH = 9;
    private static final int MAX_ITEM_ROWS = 5;

    private final GuiContext context;

    public AdminWarpListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        List<Warp> warps = context.warpService().getAll().stream()
                .sorted(Comparator.comparing(Warp::getName))
                .toList();

        int itemRows = Math.min(MAX_ITEM_ROWS, Math.max(1, (warps.size() + WIDTH - 1) / WIDTH));
        Gui gui = Gui.empty(WIDTH, itemRows + 1);

        int slot = 0;
        for (Warp warp : warps) {
            if (slot >= WIDTH * itemRows) {
                break;
            }
            gui.setItem(slot++, new SimpleItem(warpItem(warp), click -> {
                click.getEvent().setCancelled(true);
                new AdminWarpDetailGui(context).open(click.getPlayer(), warp.getName());
            }));
        }

        gui.setItem(WIDTH * itemRows, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminMenuGui(context).open(click.getPlayer());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Manage Warps")
                .setGui(gui));
        window.open();
    }

    private static ItemProvider warpItem(Warp warp) {
        Component name = Component.text(warp.getName(), warp.isEnabled() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY);
        return GuiItems.of(Material.ENDER_PEARL, name,
                Component.text("Group: " + (warp.getGroupName() == null ? "public" : warp.getGroupName()), NamedTextColor.GRAY),
                Component.text(warp.isEnabled() ? "Enabled" : "Disabled", warp.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }
}
