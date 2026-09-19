package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.spawn.Spawn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.function.Consumer;
import java.util.stream.IntStream;

/** Every world with a custom spawn set, paginated, click to adjust its location. */
public final class AdminSpawnListGui {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 6;
    private static final int CONTROL_ROW = WIDTH * (HEIGHT - 1);
    private static final int[] CONTENT_SLOTS = IntStream.range(0, CONTROL_ROW).toArray();
    private static final int BACK_SLOT = CONTROL_ROW;
    private static final int PREV_SLOT = CONTROL_ROW + 3;
    private static final int NEXT_SLOT = CONTROL_ROW + 5;

    private final GuiContext context;

    public AdminSpawnListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        List<Item> items = context.spawnService().getAll().stream()
                .sorted(Comparator.comparing(Spawn::getWorldName))
                .<Item>map(this::spawnItem)
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

        gui.setItem(NEXT_SLOT, new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> pagedGui) {
                return GuiItems.of(Material.ARROW, Component.text(
                        pagedGui.hasNextPage() ? "Next Page" : "No Next Page", NamedTextColor.GRAY));
            }
        });

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Manage Spawns")
                .setGui(gui));
        window.open();
    }

    private SimpleItem spawnItem(Spawn spawn) {
        ItemProvider provider = GuiItems.of(Material.GRASS_BLOCK,
                Component.text(spawn.getWorldName(), NamedTextColor.GOLD),
                Component.text("Permission: " + (spawn.getPermission() == null ? "public" : spawn.getPermission()), NamedTextColor.GRAY));
        return new SimpleItem(provider, click -> {
            click.getEvent().setCancelled(true);
            openAdjust(click.getPlayer(), spawn);
        });
    }

    private void openAdjust(Player admin, Spawn spawn) {
        new LocationAdjustGui(context).open(admin, new LocationAdjustGui.Target() {
            @Override
            public String getDisplayName() {
                return spawn.getWorldName();
            }

            @Override
            public Location getCurrentLocation() {
                return spawn.getLocation().toBukkitLocation(Bukkit.getWorld(spawn.getWorldName()));
            }

            @Override
            public void save(Location newLocation, Consumer<Boolean> callback) {
                context.spawnService().setSpawn(Bukkit.getWorld(spawn.getWorldName()), newLocation, admin.getUniqueId())
                        .whenComplete((unused, throwable) -> callback.accept(throwable == null));
            }
        }, () -> open(admin));
    }
}
