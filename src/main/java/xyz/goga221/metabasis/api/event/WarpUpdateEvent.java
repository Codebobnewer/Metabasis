package xyz.goga221.metabasis.api.event;

import xyz.goga221.metabasis.warp.Warp;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after an existing warp's data changes — a moved location, group assignment,
 * enabled/disabled state, fade settings, or warmup. Not fired for a brand-new warp
 * (see {@link WarpCreateEvent}) or a deletion (see {@link WarpDeleteEvent}).
 */
public final class WarpUpdateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Warp warp;

    public WarpUpdateEvent(Warp warp) {
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
