package xyz.goga221.metabasis.util;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class SafeTeleport {

    private SafeTeleport() {
    }

    /**
     * Teleports the player using Entity#teleportAsync, the Folia/Paper-safe way to cross
     * region/world boundaries, then hops back to the player's own thread via the scheduler
     * before invoking the callback.
     */
    public static void to(Player player, Location location, TaskScheduler scheduler, Consumer<Boolean> callback) {
        player.teleportAsync(location).thenAccept(success ->
                scheduler.runTask(player, () -> callback.accept(success)));
    }
}
