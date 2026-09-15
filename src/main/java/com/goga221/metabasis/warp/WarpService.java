package com.goga221.metabasis.warp;

import com.goga221.metabasis.group.GroupData;
import com.goga221.metabasis.group.GroupService;
import com.goga221.metabasis.group.GroupedWarp;
import com.goga221.metabasis.location.LocationSnapshot;
import com.goga221.metabasis.util.Names;
import com.goga221.metabasis.util.Permissions;
import com.goga221.metabasis.util.SafeTeleport;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
    public static final Set<String> RESERVED_NAMES = Set.of("set", "del", "list", "group");

    private final WarpRepository repository;
    private final GroupService groupService;
    private final TaskScheduler scheduler;
    private final Map<String, Warp> cache = new ConcurrentHashMap<>();

    public WarpService(WarpRepository repository, GroupService groupService, TaskScheduler scheduler) {
        this.repository = repository;
        this.groupService = groupService;
        this.scheduler = scheduler;
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

        String previousGroup = Optional.ofNullable(cache.get(name)).map(Warp::getGroupName).orElse(null);

        Warp warp = new Warp(
                name,
                LocationSnapshot.from(location),
                creator,
                System.currentTimeMillis(),
                previousGroup
        );

        cache.put(name, warp);
        addToStorage(warp, onSaved);
    }

    public void deleteWarp(String rawName, Consumer<Boolean> callback) {
        String name = normalize(rawName);
        Warp removed = cache.remove(name);
        if (removed == null) {
            callback.accept(false);
            return;
        }
        removeFromStorage(removed, callback);
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
        return new GroupedWarp(warp.getName(), warp.getLocation(), warp.getCreator(), warp.getCreatedAt());
    }

    private static Warp toWarp(GroupedWarp warp, String groupName) {
        return new Warp(warp.getName(), warp.getLocation(), warp.getCreator(), warp.getCreatedAt(), groupName);
    }

    /**
     * Moves every cached warp assigned to the given group back into warps.yml (making it public
     * again), then runs {@code onComplete} once all saves finish. Used before deleting a group
     * outright, since the group's own file (and its now-stale embedded warps) is about to go away.
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
            cache.put(updated.getName(), updated);
            repository.save(updated, saved -> {
                if (remaining.decrementAndGet() == 0) {
                    onComplete.run();
                }
            });
        }
    }

    /**
     * Whether the given player is allowed to warp to the given warp, based on its assigned
     * group's permission. Server operators always pass, regardless of group assignment. A group
     * with no permission set (public) is accessible to everyone.
     */
    public boolean canAccess(Player player, Warp warp) {
        String groupName = warp.getGroupName();
        if (groupName == null) {
            return true;
        }
        return groupService.get(groupName)
                .map(group -> group.getPermission() == null || Permissions.check(player, group.getPermission()))
                .orElse(true);
    }

    public void teleport(Player player, Warp warp, Consumer<Boolean> callback) {
        World world = Bukkit.getWorld(warp.getLocation().getWorldName());
        if (world == null) {
            callback.accept(false);
            return;
        }

        Location location = warp.getLocation().toBukkitLocation(world);
        SafeTeleport.to(player, location, scheduler, callback);
    }

    public static String normalize(String name) {
        return Names.normalize(name);
    }
}
