package xyz.goga221.metabasis.api;

import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.warp.Warp;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The public, stable surface other plugins can use to interact with Metabasis. Fetch it with:
 * <pre>{@code Bukkit.getServicesManager().load(MetabasisAPI.class)}</pre>
 * Backed by Metabasis's internal services, but deliberately kept separate from them — internal
 * refactors to {@code WarpService}/{@code GroupService} won't break callers of this interface.
 */
public interface MetabasisAPI {

    Optional<Warp> getWarp(String name);

    Collection<Warp> getAllWarps();

    Optional<Group> getGroup(String name);

    Collection<Group> getAllGroups();

    /** Whether the given player is currently allowed to warp to the given warp. */
    boolean canAccess(Player player, Warp warp);

    /**
     * Teleports the player to the named warp, the same as running {@code /warp <name>} would.
     *
     * @param override if true, skips the group-permission check (the warp must still be enabled)
     * @param callback receives whether the teleport actually happened
     */
    void teleportToWarp(Player player, String warpName, boolean override, Consumer<Boolean> callback);
}
