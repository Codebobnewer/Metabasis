package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.warp.Warp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/** One warp's admin screen: enable/disable, group assignment, location adjustment, delete. */
public final class AdminWarpDetailGui {

    private static final int GROUP_PICKER_WIDTH = 9;
    private static final int GROUP_PICKER_HEIGHT = 6;
    private static final int GROUP_PICKER_CONTROL_ROW = GROUP_PICKER_WIDTH * (GROUP_PICKER_HEIGHT - 1);
    private static final int[] GROUP_PICKER_CONTENT_SLOTS = IntStream.range(0, GROUP_PICKER_CONTROL_ROW).toArray();
    private static final int GROUP_PICKER_BACK_SLOT = GROUP_PICKER_CONTROL_ROW;
    private static final int GROUP_PICKER_PREV_SLOT = GROUP_PICKER_CONTROL_ROW + 3;
    private static final int GROUP_PICKER_NEXT_SLOT = GROUP_PICKER_CONTROL_ROW + 5;

    private final GuiContext context;

    public AdminWarpDetailGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin, String warpName) {
        Optional<Warp> maybeWarp = context.warpService().get(warpName);
        if (maybeWarp.isEmpty()) {
            admin.sendMessage(Component.text("That warp no longer exists.", NamedTextColor.RED));
            new AdminWarpListGui(context).open(admin);
            return;
        }
        Warp warp = maybeWarp.get();

        Gui gui = Gui.empty(9, 2);

        gui.setItem(0, new SimpleItem(GuiItems.of(
                warp.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text(warp.isEnabled() ? "Disable" : "Enable", warp.isEnabled() ? NamedTextColor.RED : NamedTextColor.GREEN)),
                click -> {
                    click.getEvent().setCancelled(true);
                    Player player = click.getPlayer();
                    context.warpService().setWarpEnabled(warp.getName(), !warp.isEnabled(), saved ->
                            context.onPlayerThread(player, () -> open(player, warpName)));
                }));

        gui.setItem(1, new SimpleItem(GuiItems.of(Material.WRITABLE_BOOK, Component.text("Assign Group", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            openGroupPicker(click.getPlayer(), warp);
        }));

        gui.setItem(2, new SimpleItem(GuiItems.of(Material.COMPASS, Component.text("Adjust Location", NamedTextColor.AQUA)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            new LocationAdjustGui(context).open(player, new LocationAdjustGui.Target() {
                @Override
                public String getDisplayName() {
                    return warp.getName();
                }

                @Override
                public Location getCurrentLocation() {
                    return warp.getLocation().toBukkitLocation(Bukkit.getWorld(warp.getLocation().getWorldName()));
                }

                @Override
                public void save(Location newLocation, java.util.function.Consumer<Boolean> callback) {
                    context.warpService().createWarp(warp.getName(), newLocation, player.getUniqueId(), callback);
                }
            }, () -> open(player, warpName));
        }));

        gui.setItem(3, new SimpleItem(GuiItems.of(Material.TNT, Component.text("Delete", NamedTextColor.RED),
                Component.text("Click again to confirm", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            confirmDelete(click.getPlayer(), warp);
        }));

        gui.setItem(8, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminWarpListGui(context).open(click.getPlayer());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Warp: " + warp.getName())
                .setGui(gui));
        window.open();
    }

    private void confirmDelete(Player admin, Warp warp) {
        Gui gui = Gui.empty(9, 1);

        gui.setItem(3, new SimpleItem(GuiItems.of(Material.RED_WOOL, Component.text("Yes, delete it", NamedTextColor.RED)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            context.warpService().deleteWarp(warp.getName(), deleted -> context.onPlayerThread(player, () -> {
                player.sendMessage(Component.text(deleted ? "Warp deleted." : "Failed to delete warp.",
                        deleted ? NamedTextColor.GREEN : NamedTextColor.RED));
                player.closeInventory();
                new AdminWarpListGui(context).open(player);
            }));
        }));

        gui.setItem(5, new SimpleItem(GuiItems.of(Material.LIME_WOOL, Component.text("Cancel", NamedTextColor.GREEN)), click -> {
            click.getEvent().setCancelled(true);
            open(click.getPlayer(), warp.getName());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Confirm delete: " + warp.getName())
                .setGui(gui));
        window.open();
    }

    private void openGroupPicker(Player admin, Warp warp) {
        List<Item> items = new ArrayList<>();
        items.add(new SimpleItem(GuiItems.of(Material.BARRIER, Component.text("None (public)", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            context.warpService().setWarpGroup(warp.getName(), null, saved ->
                    context.onPlayerThread(player, () -> open(player, warp.getName())));
        }));
        context.groupService().getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .forEach(group -> items.add(new SimpleItem(GuiItems.of(Material.PAPER, Component.text(group.getName(), NamedTextColor.GOLD)), click -> {
                    click.getEvent().setCancelled(true);
                    Player player = click.getPlayer();
                    context.warpService().setWarpGroup(warp.getName(), group.getName(), saved ->
                            context.onPlayerThread(player, () -> open(player, warp.getName())));
                })));

        PagedGui<Item> gui = PagedGui.ofItems(GROUP_PICKER_WIDTH, GROUP_PICKER_HEIGHT, items, GROUP_PICKER_CONTENT_SLOTS);

        gui.setItem(GROUP_PICKER_BACK_SLOT, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            open(click.getPlayer(), warp.getName());
        }));

        gui.setItem(GROUP_PICKER_PREV_SLOT, new PageItem(false) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasPreviousPage() ? "Previous Page" : "No Previous Page", NamedTextColor.GRAY));
            }
        });

        gui.setItem(GROUP_PICKER_NEXT_SLOT, new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasNextPage() ? "Next Page" : "No Next Page", NamedTextColor.GRAY));
            }
        });

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Assign group: " + warp.getName())
                .setGui(gui));
        window.open();
    }
}
