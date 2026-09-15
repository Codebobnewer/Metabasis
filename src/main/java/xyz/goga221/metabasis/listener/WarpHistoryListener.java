package xyz.goga221.metabasis.listener;

import xyz.goga221.metabasis.api.event.WarpCreateEvent;
import xyz.goga221.metabasis.api.event.WarpDeleteEvent;
import xyz.goga221.metabasis.api.event.WarpTeleportEvent;
import xyz.goga221.metabasis.api.event.WarpUpdateEvent;
import xyz.goga221.metabasis.history.WarpHistoryRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Records Metabasis's own warp events into the history database. Kept as a separate listener
 * (rather than logging directly from WarpService) so WarpService itself stays unaware that
 * history-tracking exists — it's purely a side effect of the events it already fires.
 */
public final class WarpHistoryListener implements Listener {

    private final WarpHistoryRepository history;

    public WarpHistoryListener(WarpHistoryRepository history) {
        this.history = history;
    }

    @EventHandler
    public void onCreate(WarpCreateEvent event) {
        history.recordEvent(event.getWarp().getName(), "CREATED", event.getWarp().getCreator(), saved -> {
        });
    }

    @EventHandler
    public void onUpdate(WarpUpdateEvent event) {
        history.recordEvent(event.getWarp().getName(), "UPDATED", event.getWarp().getCreator(), saved -> {
        });
    }

    @EventHandler
    public void onDelete(WarpDeleteEvent event) {
        history.recordEvent(event.getWarp().getName(), "DELETED", event.getWarp().getCreator(), saved -> {
        });
    }

    @EventHandler
    public void onTeleport(WarpTeleportEvent event) {
        if (!event.isCancelled()) {
            history.recordUsage(event.getWarp().getName(), event.getPlayer().getUniqueId(), saved -> {
            });
        }
    }
}
