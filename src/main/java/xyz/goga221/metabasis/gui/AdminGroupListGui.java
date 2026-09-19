package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.gui.dialog.TextInputDialogs;
import xyz.goga221.metabasis.group.Group;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Every group, paginated, click to open its detail/edit screen, plus a "Create Group" entry via Dialog. */
public final class AdminGroupListGui {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 6;
    private static final int CONTROL_ROW = WIDTH * (HEIGHT - 1);
    private static final int[] CONTENT_SLOTS = IntStream.range(0, CONTROL_ROW).toArray();
    private static final int BACK_SLOT = CONTROL_ROW;
    private static final int PREV_SLOT = CONTROL_ROW + 3;
    private static final int CREATE_SLOT = CONTROL_ROW + 4;
    private static final int NEXT_SLOT = CONTROL_ROW + 5;

    private final GuiContext context;

    public AdminGroupListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        List<Item> items = context.groupService().getAll().stream()
                .sorted(Comparator.comparing(Group::getName))
                .<Item>map(this::groupItem)
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

        gui.setItem(CREATE_SLOT, new SimpleItem(GuiItems.of(Material.EMERALD, Component.text("Create Group", NamedTextColor.GREEN)), click -> {
            click.getEvent().setCancelled(true);
            openCreateDialog(click.getPlayer());
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
                .setTitle("Manage Groups")
                .setGui(gui));
        window.open();
    }

    private void openCreateDialog(Player admin) {
        admin.closeInventory();
        TextInputDialogs.prompt(admin, "Create Group", List.of(
                new TextInputDialogs.Field("name", "Name", ""),
                new TextInputDialogs.Field("permission", "Permission (op / permission node / blank)", ""),
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

    private SimpleItem groupItem(Group group) {
        ItemProvider provider = GuiItems.of(Material.PAPER, Component.text(group.getName(), group.isEnabled() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY),
                Component.text("Permission: " + (group.getPermission() == null ? "public" : group.getPermission()), NamedTextColor.GRAY),
                Component.text(group.isEnabled() ? "Enabled" : "Disabled", group.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return new SimpleItem(provider, click -> {
            click.getEvent().setCancelled(true);
            new AdminGroupDetailGui(context).open(click.getPlayer(), group.getName());
        });
    }
}
