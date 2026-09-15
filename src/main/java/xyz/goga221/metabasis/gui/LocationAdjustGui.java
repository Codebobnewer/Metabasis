package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.util.SafeTeleport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/**
 * Nudge-based location editor shared by both warps and spawns, per the client's requirement that
 * location adjustment cover "spawn and warps" alike. The caller supplies a small {@link Target}
 * describing where the current location comes from and how to save it back — the actual
 * persistence still goes through the existing WarpService/SpawnService save paths, not a new one.
 */
public final class LocationAdjustGui {

    private static final double COARSE_STEP = 1.0;
    private static final double FINE_STEP = 0.1;
    private static final double COARSE_ROTATION_STEP = 15.0;
    private static final double FINE_ROTATION_STEP = 90.0;

    /** What's being edited: where its current location is, and how a new one gets saved. */
    public interface Target {
        String getDisplayName();

        Location getCurrentLocation();

        void save(Location newLocation, Consumer<Boolean> callback);
    }

    private final GuiContext context;

    public LocationAdjustGui(GuiContext context) {
        this.context = context;
    }

    public void open(Player admin, Target target, Runnable onBack) {
        Location working = target.getCurrentLocation().clone();
        Gui gui = Gui.empty(9, 3);

        setAxisItem(gui, 0, working, "X", Location::getX, (l, d) -> l.add(d, 0, 0));
        setAxisItem(gui, 1, working, "Y", Location::getY, (l, d) -> l.add(0, d, 0));
        setAxisItem(gui, 2, working, "Z", Location::getZ, (l, d) -> l.add(0, 0, d));
        setRotationItem(gui, 4, working, "Yaw", Location::getYaw, (l, d) -> l.setYaw((float) (l.getYaw() + d)));
        setRotationItem(gui, 5, working, "Pitch", Location::getPitch, (l, d) -> l.setPitch((float) (l.getPitch() + d)));

        gui.setItem(6, new SimpleItem(GuiItems.of(Material.ENDER_EYE, Component.text("Preview (teleport me here)", NamedTextColor.AQUA)), click -> {
            click.getEvent().setCancelled(true);
            SafeTeleport.to(click.getPlayer(), working, context.scheduler(), success -> {
            });
        }));

        gui.setItem(7, new SimpleItem(GuiItems.of(Material.LIME_DYE, Component.text("Save", NamedTextColor.GREEN)), click -> {
            click.getEvent().setCancelled(true);
            Player player = click.getPlayer();
            target.save(working, saved -> context.onPlayerThread(player, () -> {
                if (saved) {
                    player.sendMessage(Component.text("Location updated for " + target.getDisplayName() + ".", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Failed to save the new location.", NamedTextColor.RED));
                }
                player.closeInventory();
                onBack.run();
            }));
        }));

        gui.setItem(8, new SimpleItem(GuiItems.of(Material.BARRIER, Component.text("Cancel", NamedTextColor.RED)), click -> {
            click.getEvent().setCancelled(true);
            click.getPlayer().closeInventory();
            onBack.run();
        }));

        Window window = Window.single(builder -> builder
                .setViewer(admin)
                .setTitle("Adjust: " + target.getDisplayName())
                .setGui(gui));
        window.open();
    }

    /** Rebuilds the item at {@code slot} with the current value baked into its name — called again after every click. */
    private void setAxisItem(Gui gui, int slot, Location working, String axisLabel, ToDoubleFunction<Location> getter, BiConsumer<Location, Double> adjust) {
        gui.setItem(slot, new SimpleItem(GuiItems.of(Material.REDSTONE, axisName(axisLabel, getter.applyAsDouble(working))), click -> {
            click.getEvent().setCancelled(true);
            double step = click.getEvent().isShiftClick() ? FINE_STEP : COARSE_STEP;
            double delta = click.getEvent().isLeftClick() ? step : -step;
            adjust.accept(working, delta);
            setAxisItem(gui, slot, working, axisLabel, getter, adjust);
        }));
    }

    private void setRotationItem(Gui gui, int slot, Location working, String label, ToDoubleFunction<Location> getter, BiConsumer<Location, Double> adjust) {
        gui.setItem(slot, new SimpleItem(GuiItems.of(Material.COMPASS, axisName(label, getter.applyAsDouble(working))), click -> {
            click.getEvent().setCancelled(true);
            double step = click.getEvent().isShiftClick() ? FINE_ROTATION_STEP : COARSE_ROTATION_STEP;
            double delta = click.getEvent().isLeftClick() ? step : -step;
            adjust.accept(working, delta);
            setRotationItem(gui, slot, working, label, getter, adjust);
        }));
    }

    private static Component axisName(String axis, double value) {
        return Component.text(axis + ": " + String.format("%.2f", value), NamedTextColor.YELLOW);
    }
}
