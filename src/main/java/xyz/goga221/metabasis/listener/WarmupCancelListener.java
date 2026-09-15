package xyz.goga221.metabasis.listener;

import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * Cancels a player's in-progress warp warmup if they take damage or move away from where the
 * warmup started — the standard "don't move or you lose it" warp-plugin convention, so players
 * can't tank a hit or flee combat and still teleport away on schedule.
 */
public final class WarmupCancelListener implements Listener {

    /** Small threshold so looking around (a pure yaw/pitch change) doesn't cancel a warmup. */
    private static final double MOVE_CANCEL_DISTANCE_SQUARED = 0.6 * 0.6;

    private final WarpService warpService;
    private final MessageService messages;

    public WarmupCancelListener(WarpService warpService, MessageService messages) {
        this.warpService = warpService;
        this.messages = messages;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancelIfPending(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!warpService.hasPendingWarmup(player.getUniqueId())) {
            return;
        }

        Optional<Location> start = warpService.getWarmupStartLocation(player.getUniqueId());
        Location to = event.getTo();
        if (start.isEmpty() || to == null || start.get().getWorld() != to.getWorld()) {
            return;
        }

        if (start.get().distanceSquared(to) >= MOVE_CANCEL_DISTANCE_SQUARED) {
            cancelIfPending(player);
        }
    }

    /** Cleans up a pending warmup on disconnect — no message needed, the player is already gone. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warpService.cancelWarmup(event.getPlayer().getUniqueId());
    }

    private void cancelIfPending(Player player) {
        if (warpService.cancelWarmup(player.getUniqueId())) {
            messages.send(player, "warp.warmup.cancelled");
        }
    }
}
