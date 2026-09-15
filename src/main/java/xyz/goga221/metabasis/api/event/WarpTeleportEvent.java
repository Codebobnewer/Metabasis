package xyz.goga221.metabasis.api.event;

import xyz.goga221.metabasis.warp.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired right before a player is teleported to a warp — after Metabasis's own checks (access
 * permission, enabled state, warmup) have already passed. Cancelling this prevents the teleport;
 * the destination can also be redirected.
 */
public final class WarpTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Warp warp;
    private Location destination;
    private boolean cancelled;

    public WarpTeleportEvent(Player player, Warp warp, Location destination) {
        this.player = player;
        this.warp = warp;
        this.destination = destination;
    }

    public Player getPlayer() {
        return player;
    }

    public Warp getWarp() {
        return warp;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
