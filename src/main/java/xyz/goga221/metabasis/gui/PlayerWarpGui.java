package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.gui.dialog.TextInputDialogs;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpFilter;
import xyz.goga221.metabasis.warp.WarpService;
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

/**
 * The player-facing warp browser ({@code /warp gui}) — every warp the viewer can currently
 * access, sorted by group and paginated, with a search box matching warp name or group name,
 * click to teleport.
 */
public final class PlayerWarpGui {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 6;
    private static final int CONTROL_ROW = WIDTH * (HEIGHT - 1);
    private static final int[] CONTENT_SLOTS = IntStream.range(0, CONTROL_ROW).toArray();
    private static final int CLOSE_SLOT = CONTROL_ROW;
    private static final int PREV_SLOT = CONTROL_ROW + 3;
    private static final int SEARCH_SLOT = CONTROL_ROW + 4;
    private static final int NEXT_SLOT = CONTROL_ROW + 5;

    private final WarpService warpService;
    private final MessageService messages;

    public PlayerWarpGui(WarpService warpService, MessageService messages) {
        this.warpService = warpService;
        this.messages = messages;
    }

    public void open(Player player) {
        open(player, null);
    }

    public void open(Player player, String searchQuery) {
        List<Item> items = WarpFilter.search(warpService.getAll(), searchQuery).stream()
                .filter(warp -> warpService.canAccess(player, warp))
                .<Item>map(this::warpItem)
                .toList();

        PagedGui<Item> gui = PagedGui.ofItems(WIDTH, HEIGHT, items, CONTENT_SLOTS);

        gui.setItem(CLOSE_SLOT, new SimpleItem(GuiItems.of(Material.BARRIER, Component.text("Close", NamedTextColor.RED)), click -> {
            click.getEvent().setCancelled(true);
            click.getPlayer().closeInventory();
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
            Player viewer = click.getPlayer();
            viewer.closeInventory();
            TextInputDialogs.promptText(viewer, "Search Warps", "Warp or group name (blank = all)", searchQuery,
                    query -> open(viewer, query));
        }));

        gui.setItem(NEXT_SLOT, new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasNextPage() ? "Next Page" : "No Next Page", NamedTextColor.GRAY));
            }
        });

        Window window = Window.single(builder -> builder
                .setViewer(player)
                .setTitle(searchQuery == null || searchQuery.isBlank() ? "Warps" : "Warps: " + searchQuery)
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
        Component name = Component.text(warp.getName(), NamedTextColor.GOLD);
        ItemProvider provider = GuiItems.of(Material.ENDER_PEARL, name,
                Component.text("Group: " + WarpFilter.groupLabel(warp), NamedTextColor.GRAY));
        return new SimpleItem(provider, click -> {
            click.getEvent().setCancelled(true);
            click.getPlayer().closeInventory();
            warpService.teleport(click.getPlayer(), warp, success ->
                    messages.send(click.getPlayer(), success ? "warp.teleport.success" : "warp.teleport.failed", Messages.name(warp.getName())));
        });
    }
}
