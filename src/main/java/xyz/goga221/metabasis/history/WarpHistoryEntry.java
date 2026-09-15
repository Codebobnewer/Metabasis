package xyz.goga221.metabasis.history;

import java.util.UUID;

/**
 * One row from the warp_events table. {@code actor} is null when the event predates attribution
 * or came from an action the recording listener couldn't tie to a specific player.
 */
public record WarpHistoryEntry(String warpName, String eventType, UUID actor, long timestamp) {
}
