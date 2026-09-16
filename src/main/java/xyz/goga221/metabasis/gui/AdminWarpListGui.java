package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.gui.dialog.TextInputDialogs;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.stream.IntStream;

/** Every warp, sorted by group and paginated, with a search box matching warp name or group name — click to open its detail/edit screen. */
public final class AdminWarpListGui {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 6;
    private static final int CONTROL_ROW = WIDTH * (HEIGHT - 1);
    private static final int[] CONTENT_SLOTS = IntStream.range(0, CONTROL_ROW).toArray();
    private static final int BACK_SLOT = CONTROL_ROW;
    private static final int PREV_SLOT = CONTROL_ROW + 3;
    private static final int SEARCH_SLOT = CONTROL_ROW + 4;
    private static final int NEXT_SLOT = CONTROL_ROW + 5;

    private final GuiContext context;

    public AdminWarpListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        open(admin, null);
    }

    public void open(Player admin, String searchQuery) {
        List<Item> items = WarpFilter.search(context.warpService().getAll(), searchQuery).stream()
                .<Item>map(this::warpItem)
                .toList();

        PagedGui<Item> gui = PagedGui.ofItems(WIDTH, HEIGHT, items, CONTENT_SLOTS);

        gui.setItem(BACK_SLOT, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminMenuGui(context).open(click.getPlayer());
        }));

        gui.setItem(PREV_SLOT, new PageItem(false) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasPreviousPage() ? "Previous Page" : "No Previous Page", NamedTextColor.GRAY));
            }
        });

        gui.setItem(SEARCH_SLOT, new SimpleItem(searchItem(searchQuery), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            player.closeInventory();
            TextInputDialogs.promptText(player, "Search Warps", "Warp or group name (blank = all)", searchQuery,
                    query -> open(player, query));
        }));

        gui.setItem(NEXT_SLOT, new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasNextPage() ? "Next Page" : "No Next Page", NamedTextColor.GRAY));
            }
        });

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle(searchQuery == null || searchQuery.isBlank() ? "Manage Warps" : "Manage Warps: " + searchQuery)
                .setGui(gui));
        window.open();
    }

    private static ItemProvider searchItem(String searchQuery) {
        Component name = Component.text("Search", NamedTextColor.AQUA);
        if (searchQuery == null || searchQuery.isBlank()) {
            return GuiItems.of(Material.COMPASS, name, Component.text("Showing all warps", NamedTextColor.GRAY));
        }
        return GuiItems.of(Material.COMPASS, name,
                Component.text("Filter: " + searchQuery, NamedTextColor.GRAY),
                Component.text("Click to change", NamedTextColor.DARK_GRAY));
    }

    private SimpleItem warpItem(Warp warp) {
        ItemProvider provider = warpItemProvider(warp);
        return new SimpleItem(provider, click -> {
            click.getEvent().setCancelled(true);
            new AdminWarpDetailGui(context).open(click.getPlayer(), warp.getName());
        });
    }

    private static ItemProvider warpItemProvider(Warp warp) {
        Component name = Component.text(warp.getName(), warp.isEnabled() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY);
        return GuiItems.of(Material.ENDER_PEARL, name,
                Component.text("Group: " + WarpFilter.groupLabel(warp), NamedTextColor.GRAY),
                Component.text(warp.isEnabled() ? "Enabled" : "Disabled", warp.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }
}
