package xyz.goga221.metabasis.api.event;

import xyz.goga221.metabasis.warp.Warp;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a warp is deleted — including each warp removed by a cascading group delete. */
public final class WarpDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Warp warp;

    public WarpDeleteEvent(Warp warp) {
        this.warp = warp;
    }

    public Warp getWarp() {
        return warp;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
