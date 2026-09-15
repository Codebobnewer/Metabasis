package xyz.goga221.metabasis.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/** The admin GUI's landing screen (`/warp admin`) — entry points into warp and group management. */
public final class AdminMenuGui {

    private final GuiContext context;

    public AdminMenuGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        Gui gui = Gui.empty(9, 1);

        gui.setItem(1, new SimpleItem(GuiItems.of(Material.ENDER_PEARL, Component.text("Manage Warps", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            new AdminWarpListGui(context).open(click.getPlayer());
        }));
        gui.setItem(3, new SimpleItem(GuiItems.of(Material.WRITABLE_BOOK, Component.text("Manage Groups", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            new AdminGroupListGui(context).open(click.getPlayer());
        }));
        gui.setItem(5, new SimpleItem(GuiItems.of(Material.GRASS_BLOCK, Component.text("Manage Spawns", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            new AdminSpawnListGui(context).open(click.getPlayer());
        }));
        gui.setItem(7, new SimpleItem(GuiItems.of(Material.BARRIER, Component.text("Close", NamedTextColor.RED)), click -> {
            click.getEvent().setCancelled(true);
            click.getPlayer().closeInventory();
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Metabasis Admin")
                .setGui(gui));
        window.open();
    }
}
