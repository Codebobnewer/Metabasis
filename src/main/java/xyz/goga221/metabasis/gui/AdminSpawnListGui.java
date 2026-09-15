package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.spawn.Spawn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Every world with a custom spawn set, click to adjust its location. */
public final class AdminSpawnListGui {

    private static final int WIDTH = 9;
    private static final int MAX_ITEM_ROWS = 5;

    private final GuiContext context;

    public AdminSpawnListGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin) {
        List<Spawn> spawns = context.spawnService().getAll().stream()
                .sorted(Comparator.comparing(Spawn::getWorldName))
                .toList();

        int itemRows = Math.min(MAX_ITEM_ROWS, Math.max(1, (spawns.size() + WIDTH - 1) / WIDTH));
        Gui gui = Gui.empty(WIDTH, itemRows + 1);

        int slot = 0;
        for (Spawn spawn : spawns) {
            if (slot >= WIDTH * itemRows) {
                break;
            }
            gui.setItem(slot++, new SimpleItem(GuiItems.of(Material.GRASS_BLOCK,
                    Component.text(spawn.getWorldName(), NamedTextColor.GOLD),
                    Component.text("Permission: " + (spawn.getPermission() == null ? "public" : spawn.getPermission()), NamedTextColor.GRAY)),
                    click -> {
                        click.getEvent().setCancelled(true);
                        openAdjust(click.getPlayer(), spawn);
                    }));
        }

        gui.setItem(WIDTH * itemRows, new SimpleItem(GuiItems.of(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)), click -> {
            click.getEvent().setCancelled(true);
            new AdminMenuGui(context).open(click.getPlayer());
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Manage Spawns")
                .setGui(gui));
        window.open();
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
