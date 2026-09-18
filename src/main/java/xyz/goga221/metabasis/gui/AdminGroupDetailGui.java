package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.gui.dialog.TextInputDialogs;
import xyz.goga221.metabasis.group.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Optional;

/** One group's admin screen: enable/disable, rebind permission, edit description, delete. */
public final class AdminGroupDetailGui {

    private final GuiContext context;

    public AdminGroupDetailGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin, String groupName) {
        Optional<Group> maybeGroup = context.groupService().get(groupName);
        if (maybeGroup.isEmpty()) {
            admin.sendMessage(Component.text("That group no longer exists.", NamedTextColor.RED));
            new AdminGroupListGui(context).open(admin);
            return;
        }
        Group group = maybeGroup.get();

        Gui gui = Gui.empty(9, 2);

        gui.setItem(0, new SimpleItem(GuiItems.of(
                group.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text(group.isEnabled() ? "Disable" : "Enable", group.isEnabled() ? NamedTextColor.RED : NamedTextColor.GREEN)),
                click -> {
                    click.getEvent().setCancelled(true);
                    Player player = click.getPlayer();
                    context.groupService().setEnabled(group.getName(), !group.isEnabled())
                            .whenComplete((unused, throwable) -> context.onPlayerThread(player, () -> open(player, groupName)));
                }));

        gui.setItem(1, new SimpleItem(GuiItems.of(Material.NAME_TAG, Component.text("Rebind Permission", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            player.closeInventory();
            TextInputDialogs.promptText(player, "Rebind Permission", "op / permission node / blank", group.getPermission(), permission ->
                    context.groupService().updatePermission(group.getName(), permission)
                            .whenComplete((unused, throwable) -> context.onPlayerThread(player, () -> {
                                if (throwable != null) {
                                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                                    player.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
                                } else {
                                    player.sendMessage(Component.text("Permission updated.", NamedTextColor.GREEN));
                                }
                                open(player, groupName);
                            })));
        }));

        gui.setItem(2, new SimpleItem(GuiItems.of(Material.WRITABLE_BOOK, Component.text("Edit Description", NamedTextColor.GOLD)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            player.closeInventory();
            TextInputDialogs.promptText(player, "Edit Description", "Description", group.getDescription(), description ->
                    context.groupService().updateDescription(group.getName(), description)
                            .whenComplete((unused, throwable) -> context.onPlayerThread(player, () -> {
                                if (throwable != null) {
                                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                                    player.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
                                } else {
                                    player.sendMessage(Component.text("Description updated.", NamedTextColor.GREEN));
                                }
                                open(player, groupName);
                            })));
        }));

        gui.setItem(3, new SimpleItem(GuiItems.of(Material.TNT, Component.text("Delete", NamedTextColor.RED),
                Component.text("Click again to confirm", NamedTextColor.GRAY),
                Component.text("Deletes every warp in this group too", NamedTextColor.DARK_RED)), click -> {
            click.getEvent().setCancelled(true);
            confirmDelete(click.getPlayer(), group);
        }));

        gui.setItem(8, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminGroupListGui(context).open(click.getPlayer());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Group: " + group.getName())
                .setGui(gui));
        window.open();
    }

    private void confirmDelete(Player admin, Group group) {
        Gui gui = Gui.empty(9, 1);

        gui.setItem(3, new SimpleItem(GuiItems.of(Material.RED_WOOL, Component.text("Yes, delete it", NamedTextColor.RED)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            context.warpService().deleteWarpsInGroup(group.getName(), () ->
                    context.groupService().deleteGroup(group.getName())
                            .whenComplete((existed, throwable) -> context.onPlayerThread(player, () -> {
                                player.sendMessage(Component.text(
                                        Boolean.TRUE.equals(existed) ? "Group deleted, along with its warps." : "Failed to delete group.",
                                        Boolean.TRUE.equals(existed) ? NamedTextColor.GREEN : NamedTextColor.RED));
                                player.closeInventory();
                                new AdminGroupListGui(context).open(player);
                            })));
        }));

        gui.setItem(5, new SimpleItem(GuiItems.of(Material.LIME_WOOL, Component.text("Cancel", NamedTextColor.GREEN)), click -> {
            click.getEvent().setCancelled(true);
            open(click.getPlayer(), group.getName());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Confirm delete: " + group.getName())
                .setGui(gui));
        window.open();
    }
}
