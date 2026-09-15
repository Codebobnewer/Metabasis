package xyz.goga221.metabasis.history;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface WarpHistoryRepository {

    /** Records a create/update/delete event for a warp. {@code actor} may be null if unknown. */
    void recordEvent(String warpName, String eventType, UUID actor, Consumer<Boolean> callback);

    /** Records a successful (or attempted, non-cancelled) teleport to a warp. */
    void recordUsage(String warpName, UUID player, Consumer<Boolean> callback);

    /** The most recent events for a warp, newest first. */
    void getRecentEvents(String warpName, int limit, Consumer<List<WarpHistoryEntry>> onLoaded, Consumer<Throwable> onError);
}
