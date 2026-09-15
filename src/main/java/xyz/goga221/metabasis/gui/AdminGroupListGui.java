package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.gui.dialog.TextInputDialogs;
import xyz.goga221.metabasis.group.Group;
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

/** Every group, click to open its detail/edit screen, plus a "Create Group" entry via Dialog. */
public final class AdminGroupListGui {

    private static final int WIDTH = 9;
    private static final int MAX_ITEM_ROWS = 5;

    private final GuiContext context;

    public AdminGroupListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        List<Group> groups = context.groupService().getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .toList();

        int itemRows = Math.min(MAX_ITEM_ROWS, Math.max(1, (groups.size() + WIDTH - 1) / WIDTH));
        Gui gui = Gui.empty(WIDTH, itemRows + 1);

        int slot = 0;
        for (Group group : groups) {
            if (slot >= WIDTH * itemRows) {
                break;
            }
            gui.setItem(slot++, new SimpleItem(groupItem(group), click -> {
                click.getEvent().setCancelled(true);
                new AdminGroupDetailGui(context).open(click.getPlayer(), group.getName());
            }));
        }

        int lastRow = WIDTH * itemRows;
        gui.setItem(lastRow, new SimpleItem(GuiItems.of(Material.EMERALD, Component.text("Create Group", NamedTextColor.GREEN)), click -> {
            click.getEvent().setCancelled(true);
            openCreateDialog(click.getPlayer());
        }));
        gui.setItem(lastRow + 1, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminMenuGui(context).open(click.getPlayer());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Manage Groups")
                .setGui(gui));
        window.open();
    }

    private void openCreateDialog(Player admin) {
        admin.closeInventory();
        TextInputDialogs.prompt(admin, "Create Group", List.of(
                new TextInputDialogs.Field("name", "Name", ""),
                new TextInputDialogs.Field("permission", "Permission (op / LuckPerms group / blank)", ""),
                new TextInputDialogs.Field("description", "Description (optional)", "")
        ), values -> {
            String name = values.get("name");
            if (name == null) {
                context.onPlayerThread(admin, () -> {
                    admin.sendMessage(Component.text("A group name is required.", NamedTextColor.RED));
                    open(admin);
                });
                return;
            }
            context.groupService().createGroup(name, values.get("permission"), values.get("description"))
                    .whenComplete((unused, throwable) -> context.onPlayerThread(admin, () -> {
                        if (throwable != null) {
                            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                            admin.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
                        } else {
                            admin.sendMessage(Component.text("Group created.", NamedTextColor.GREEN));
                        }
                        open(admin);
                    }));
        });
    }

    private static ItemProvider groupItem(Group group) {
        return GuiItems.of(Material.PAPER, Component.text(group.getName(), group.isEnabled() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY),
                Component.text("Permission: " + (group.getPermission() == null ? "public" : group.getPermission()), NamedTextColor.GRAY),
                Component.text(group.isEnabled() ? "Enabled" : "Disabled", group.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }
}
