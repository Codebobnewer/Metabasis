package xyz.goga221.metabasis.warp;

import xyz.goga221.metabasis.api.event.WarpCreateEvent;
import xyz.goga221.metabasis.api.event.WarpDeleteEvent;
import xyz.goga221.metabasis.api.event.WarpTeleportEvent;
import xyz.goga221.metabasis.api.event.WarpUpdateEvent;
import xyz.goga221.metabasis.group.GroupData;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.group.GroupedWarp;
import xyz.goga221.metabasis.location.LocationSnapshot;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.util.Names;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.util.SafeTeleport;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class WarpService {

    /** Names that collide with /warp's own subcommand literals and can't be used for a warp. */
    public static final Set<String> RESERVED_NAMES = Set.of("create", "move", "del", "list", "group", "massport", "enable", "disable", "fade", "warmup", "history", "gui", "admin");

    /** Ticks per second, for converting warp fade/warmup durations to scheduler delays. */
    private static final long TICKS_PER_SECOND = 20L;
    private static final long MILLIS_PER_TICK = 50L;

    private final WarpRepository repository;
    private final GroupService groupService;
    private final TaskScheduler scheduler;
    private final MessageService messages;
    private final Map<String, Warp> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PendingWarmup> pendingWarmups = new ConcurrentHashMap<>();

    public WarpService(WarpRepository repository, GroupService groupService, TaskScheduler scheduler, MessageService messages) {
        this.repository = repository;
        this.groupService = groupService;
        this.scheduler = scheduler;
        this.messages = messages;
    }

    /**
     * Loads every warp into the cache: ungrouped warps from warps.yml, plus every warp embedded
     * in a group's file (from the already-loaded {@code groupData} — group loading must happen
     * before this, since grouped warps' data lives only in their group's file).
     */
    public void loadAllIntoCache(List<GroupData> groupData, Runnable onLoaded, Consumer<Throwable> onError) {
        repository.loadAll(ungroupedWarps -> {
            cache.clear();
            for (Warp warp : ungroupedWarps) {
                cache.put(warp.getName(), warp);
            }
            for (GroupData data : groupData) {
                String groupName = data.getGroup().getName();
                for (GroupedWarp warp : data.getWarps()) {
                    cache.put(warp.getName(), toWarp(warp, groupName));
                }
            }
            onLoaded.run();
        }, onError);
    }

    public boolean exists(String name) {
        return cache.containsKey(normalize(name));
    }

    public Optional<Warp> get(String name) {
        return Optional.ofNullable(cache.get(normalize(name)));
    }

    public Collection<Warp> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public void createWarp(String rawName, Location location, UUID creator, Consumer<Boolean> onSaved) {
        String name = normalize(rawName);
        if (RESERVED_NAMES.contains(name)) {
            throw new IllegalArgumentException("'" + name + "' is a reserved name and can't be used for a warp.");
        }

        Warp existing = cache.get(name);
        String previousGroup = existing == null ? null : existing.getGroupName();
        boolean previousEnabled = existing == null || existing.isEnabled();
        int previousFadeIn = existing == null ? 0 : existing.getFadeInTicks();
        int previousStay = existing == null ? 0 : existing.getStayTicks();
        int previousFadeOut = existing == null ? 0 : existing.getFadeOutTicks();
        int previousWarmup = existing == null ? 0 : existing.getWarmupSeconds();

        Warp warp = new Warp(
                name,
                LocationSnapshot.from(location),
                creator,
                System.currentTimeMillis(),
                previousGroup,
                previousEnabled,
                previousFadeIn,
                previousStay,
                previousFadeOut,
                previousWarmup
        );

        cache.put(name, warp);
        if (existing == null) {
            Bukkit.getPluginManager().callEvent(new WarpCreateEvent(warp));
        } else {
            Bukkit.getPluginManager().callEvent(new WarpUpdateEvent(warp));
        }
        addToStorage(warp, onSaved);
    }

    /** Enables or disables a warp in place, without touching its location/group. */
    public void setWarpEnabled(String rawName, boolean enabled, Consumer<Boolean> callback) {
        String name = normalize(rawName);
        Warp existing = cache.get(name);
        if (existing == null) {
            throw new IllegalArgumentException("No warp named '" + name + "' exists.");
        }

        Warp updated = existing.withEnabled(enabled);
        cache.put(name, updated);
        Bukkit.getPluginManager().callEvent(new WarpUpdateEvent(updated));
        addToStorage(updated, callback);
    }

    /** Sets the title fade-in/stay/fade-out durations (ticks) shown when a player arrives at this warp. */
    public void setWarpFade(String rawName, int fadeInTicks, int stayTicks, int fadeOutTicks, Consumer<Boolean> callback) {
        String name = normalize(rawName);
        Warp existing = cache.get(name);
        if (existing == null) {
            throw new IllegalArgumentException("No warp named '" + name + "' exists.");
        }

        Warp updated = existing.withFadeInTicks(fadeInTicks).withStayTicks(stayTicks).withFadeOutTicks(fadeOutTicks);
        cache.put(name, updated);
        Bukkit.getPluginManager().callEvent(new WarpUpdateEvent(updated));
        addToStorage(updated, callback);
    }

    /** Sets how many seconds a player must stand still before teleporting to this warp (0 = instant). */
    public void setWarpWarmup(String rawName, int warmupSeconds, Consumer<Boolean> callback) {
        String name = normalize(rawName);
        Warp existing = cache.get(name);
        if (existing == null) {
            throw new IllegalArgumentException("No warp named '" + name + "' exists.");
        }

        Warp updated = existing.withWarmupSeconds(warmupSeconds);
        cache.put(name, updated);
        Bukkit.getPluginManager().callEvent(new WarpUpdateEvent(updated));
        addToStorage(updated, callback);
    }

    public void deleteWarp(String rawName, Consumer<Boolean> callback) {
        String name = normalize(rawName);
        Warp existing = cache.get(name);
        if (existing == null) {
            callback.accept(false);
            return;
        }
        // Cache/event only change once storage confirms the delete, so a failed disk write can't
        // leave the warp gone from gameplay while its file entry still exists on disk.
        removeFromStorage(existing, removed -> {
            if (removed) {
                cache.remove(name);
                Bukkit.getPluginManager().callEvent(new WarpDeleteEvent(existing));
            }
            callback.accept(removed);
        });
    }

    /** Moves a warp's data between warps.yml and its (old/new) group's file, as needed. */
    public void setWarpGroup(String rawWarpName, String groupNameOrNull, Consumer<Boolean> callback) {
        String name = normalize(rawWarpName);
        Warp existing = cache.get(name);
        if (existing == null) {
            throw new IllegalArgumentException("No warp named '" + name + "' exists.");
        }

        Warp updated = existing.withGroupName(groupNameOrNull);
        cache.put(name, updated);
        Bukkit.getPluginManager().callEvent(new WarpUpdateEvent(updated));

        removeFromStorage(existing, removed -> {
            if (!removed) {
                callback.accept(false);
                return;
            }
            addToStorage(updated, callback);
        });
    }

    /** Saves the warp to warps.yml if ungrouped, or into its assigned group's file otherwise. */
    private void addToStorage(Warp warp, Consumer<Boolean> callback) {
        if (warp.getGroupName() == null) {
            repository.save(warp, callback);
        } else {
            groupService.saveWarpInGroup(warp.getGroupName(), toGroupedWarp(warp), callback);
        }
    }

    /** Removes the warp from wherever it's currently stored (warps.yml, or its group's file). */
    private void removeFromStorage(Warp warp, Consumer<Boolean> callback) {
        if (warp.getGroupName() == null) {
            repository.delete(warp.getName(), callback);
        } else {
            groupService.removeWarpFromGroup(warp.getGroupName(), warp.getName(), callback);
        }
    }

    private static GroupedWarp toGroupedWarp(Warp warp) {
        return new GroupedWarp(warp.getName(), warp.getLocation(), warp.getCreator(), warp.getCreatedAt(), warp.isEnabled(),
                warp.getFadeInTicks(), warp.getStayTicks(), warp.getFadeOutTicks(), warp.getWarmupSeconds());
    }

    private static Warp toWarp(GroupedWarp warp, String groupName) {
        return new Warp(warp.getName(), warp.getLocation(), warp.getCreator(), warp.getCreatedAt(), groupName, warp.isEnabled(),
                warp.getFadeInTicks(), warp.getStayTicks(), warp.getFadeOutTicks(), warp.getWarmupSeconds());
    }

    /**
     * Moves every cached warp assigned to the given group back into warps.yml (making it public
     * again), then runs {@code onComplete} once all saves finish. Used by the explicit
     * {@code /warp group <name> none} un-assign action — group *deletion* uses the destructive
     * {@link #deleteWarpsInGroup} instead, per the client's cascading-delete requirement.
     */
    public void unassignWarpsInGroup(String rawGroupName, Runnable onComplete) {
        String name = GroupService.normalize(rawGroupName);
        List<Warp> affected = new ArrayList<>();
        for (Warp warp : cache.values()) {
            if (name.equals(warp.getGroupName())) {
                affected.add(warp);
            }
        }

        if (affected.isEmpty()) {
            onComplete.run();
            return;
        }

        AtomicInteger remaining = new AtomicInteger(affected.size());
        for (Warp warp : affected) {
            Warp updated = warp.withGroupName(null);
            // Remove the stale entry from the group's own file first, then write the new ungrouped
            // entry to warps.yml, and only update the cache once both succeed — otherwise a
            // leftover GroupedWarp entry silently re-attaches the warp to this group on next load.
            groupService.removeWarpFromGroup(name, warp.getName(), removedFromGroup ->
                    repository.save(updated, saved -> {
                        if (saved) {
                            cache.put(updated.getName(), updated);
                        }
                        if (remaining.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    }));
        }
    }

    /**
     * Deletes every cached warp assigned to the given group outright (not just unassigning them).
     * Only evicts them from the in-memory cache — their persisted data lives inside the group's
     * own file, which the caller deletes wholesale immediately after this, so no separate
     * per-warp storage write is needed here.
     */
    public void deleteWarpsInGroup(String rawGroupName, Runnable onComplete) {
        String name = GroupService.normalize(rawGroupName);
        List<Warp> affected = new ArrayList<>();
        for (Warp warp : cache.values()) {
            if (name.equals(warp.getGroupName())) {
                affected.add(warp);
            }
        }
        for (Warp warp : affected) {
            cache.remove(warp.getName());
            Bukkit.getPluginManager().callEvent(new WarpDeleteEvent(warp));
        }
        onComplete.run();
    }

    /**
     * Whether the given player is allowed to warp to the given warp: the warp itself must be
     * enabled, and — if it's assigned to a group — that group must also be enabled and either
     * have no permission set (public) or the player must hold it. Server operators always pass
     * the permission check, but NOT the enabled checks — a disabled warp/group is off-limits to
     * everyone, including ops, until re-enabled.
     */
    public boolean canAccess(Player player, Warp warp) {
        if (!warp.isEnabled()) {
            return false;
        }

        String groupName = warp.getGroupName();
        if (groupName == null) {
            return true;
        }
        return groupService.get(groupName)
                .map(group -> group.isEnabled()
                        && (group.getPermission() == null || Permissions.check(player, group.getPermission())))
                .orElse(true);
    }

    public void teleport(Player player, Warp warp, Consumer<Boolean> callback) {
        World world = Bukkit.getWorld(warp.getLocation().getWorldName());
        if (world == null) {
            callback.accept(false);
            return;
        }

        Location location = warp.getLocation().toBukkitLocation(world);

        WarpTeleportEvent event = new WarpTeleportEvent(player, warp, location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            callback.accept(false);
            return;
        }

        SafeTeleport.to(player, event.getDestination(), scheduler, success -> {
            if (success) {
                showFadeTitle(player, warp);
            }
            callback.accept(success);
        });
    }

    private void showFadeTitle(Player player, Warp warp) {
        if (warp.getFadeInTicks() <= 0 && warp.getStayTicks() <= 0 && warp.getFadeOutTicks() <= 0) {
            return;
        }

        Component title = messages.parse("warp.fade.title", Messages.name(warp.getName()));
        Component subtitle = messages.parse("warp.fade.subtitle", Messages.name(warp.getName()));
        Title.Times times = Title.Times.times(
                Duration.ofMillis(warp.getFadeInTicks() * MILLIS_PER_TICK),
                Duration.ofMillis(warp.getStayTicks() * MILLIS_PER_TICK),
                Duration.ofMillis(warp.getFadeOutTicks() * MILLIS_PER_TICK));
        player.showTitle(Title.title(title, subtitle, times));
    }

    /**
     * Starts a warmup countdown for the player; after {@code warmupSeconds}, runs {@code onComplete}
     * (the actual teleport) unless cancelled first via {@link #cancelWarmup}. Starting a new
     * warmup silently replaces any existing one for that player.
     */
    public void startWarmup(Player player, int warmupSeconds, Runnable onComplete) {
        UUID playerId = player.getUniqueId();
        cancelWarmup(playerId);
        MyScheduledTask task = scheduler.runTaskLater(player, () -> {
            pendingWarmups.remove(playerId);
            onComplete.run();
        }, warmupSeconds * TICKS_PER_SECOND);
        pendingWarmups.put(playerId, new PendingWarmup(player.getLocation(), task));
    }

    /** Cancels the player's pending warmup, if any, returning whether one was actually cancelled. */
    public boolean cancelWarmup(UUID playerId) {
        PendingWarmup pending = pendingWarmups.remove(playerId);
        if (pending == null) {
            return false;
        }
        pending.task().cancel();
        return true;
    }

    public boolean hasPendingWarmup(UUID playerId) {
        return pendingWarmups.containsKey(playerId);
    }

    /** The location the player was at when their current warmup started, for movement-based cancellation. */
    public Optional<Location> getWarmupStartLocation(UUID playerId) {
        return Optional.ofNullable(pendingWarmups.get(playerId)).map(PendingWarmup::startLocation);
    }

    public static String normalize(String name) {
        return Names.normalize(name);
    }

    private record PendingWarmup(Location startLocation, MyScheduledTask task) {
    }
}
