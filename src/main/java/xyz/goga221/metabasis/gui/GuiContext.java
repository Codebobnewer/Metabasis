package xyz.goga221.metabasis.gui;

import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.spawn.SpawnService;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.WarpService;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.entity.Player;

/** Shared references every admin GUI screen needs, bundled to avoid repeating five constructor params per screen. */
public record GuiContext(WarpService warpService, GroupService groupService, SpawnService spawnService,
                          MessageService messages, TaskScheduler scheduler) {

    /**
     * Runs {@code action} on the player's own region thread. WarpService/GroupService callbacks
     * resolve on an async thread (their repository saves run via {@code runTaskAsynchronously}),
     * but opening/refreshing an InvUI window is a player-entity operation — the same reasoning
     * that requires {@code SafeTeleport} to hop back before touching the player post-teleport.
     */
    public void onPlayerThread(Player player, Runnable action) {
        scheduler.runTask(player, action);
    }
}
