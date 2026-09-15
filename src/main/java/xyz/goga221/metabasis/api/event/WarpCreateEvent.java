package xyz.goga221.metabasis.api.event;

import xyz.goga221.metabasis.warp.Warp;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a brand-new warp is created — not fired when an existing warp is moved via /warp set. */
public final class WarpCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Warp warp;

    public WarpCreateEvent(Warp warp) {
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
