package xyz.goga221.metabasis.group;

import xyz.goga221.metabasis.api.event.GroupDeleteEvent;
import xyz.goga221.metabasis.util.Names;
import xyz.goga221.metabasis.util.PermissionResolver;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GroupService {

    /** Names that collide with /group's own subcommand literals and can't be used for a group. */
    public static final Set<String> RESERVED_NAMES = Set.of("create", "delete", "permission", "list", "describe", "enable", "disable");

    private final GroupRepository repository;
    private final Map<String, Group> cache = new ConcurrentHashMap<>();

    public GroupService(GroupRepository repository) {
        this.repository = repository;
    }

    /** Loads every group into the cache, returning each group's loaded data (including its embedded warps). */
    public CompletableFuture<List<GroupData>> loadAllIntoCache() {
        return repository.loadAll().thenApply(groupData -> {
            cache.clear();
            for (GroupData data : groupData) {
                cache.put(data.getGroup().getName(), data.getGroup());
            }
            return groupData;
        });
    }

    public boolean exists(String name) {
        return cache.containsKey(normalize(name));
    }

    public Optional<Group> get(String name) {
        return Optional.ofNullable(cache.get(normalize(name)));
    }

    public Collection<Group> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public CompletableFuture<Void> createGroup(String rawName, String rawPermission, String description) {
        String name = normalize(rawName);
        if (RESERVED_NAMES.contains(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("'" + name + "' is a reserved name and can't be used for a group."));
        }
        if (cache.containsKey(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("A group named '" + name + "' already exists."));
        }
        PermissionResolver.Resolution resolution = PermissionResolver.resolve(rawPermission);
        if (resolution.isRejected()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(resolution.rejectionMessage()));
        }

        Group group = new Group(name, resolution.value(), description, true);
        cache.put(name, group);
        return repository.save(group);
    }

    public CompletableFuture<Void> updatePermission(String rawName, String rawPermission) {
        String name = normalize(rawName);
        Group existing = cache.get(name);
        if (existing == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No group named '" + name + "' exists."));
        }
        PermissionResolver.Resolution resolution = PermissionResolver.resolve(rawPermission);
        if (resolution.isRejected()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(resolution.rejectionMessage()));
        }

        Group updated = existing.withPermission(resolution.value());
        cache.put(name, updated);
        return repository.save(updated);
    }

    public CompletableFuture<Void> updateDescription(String rawName, String description) {
        String name = normalize(rawName);
        Group existing = cache.get(name);
        if (existing == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No group named '" + name + "' exists."));
        }

        Group updated = existing.withDescription(description);
        cache.put(name, updated);
        return repository.save(updated);
    }

    public CompletableFuture<Void> setEnabled(String rawName, boolean enabled) {
        String name = normalize(rawName);
        Group existing = cache.get(name);
        if (existing == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No group named '" + name + "' exists."));
        }

        Group updated = existing.withEnabled(enabled);
        cache.put(name, updated);
        return repository.save(updated);
    }

    public CompletableFuture<Boolean> deleteGroup(String rawName) {
        String name = normalize(rawName);
        Group removed = cache.remove(name);
        if (removed != null) {
            Bukkit.getPluginManager().callEvent(new GroupDeleteEvent(removed));
        }
        return repository.delete(name);
    }

    /** Adds or updates one warp's entry inside the given group's file. */
    public void saveWarpInGroup(String groupName, GroupedWarp warp, Consumer<Boolean> callback) {
        repository.saveWarpEntry(groupName, warp, callback);
    }

    /** Removes one warp's entry from the given group's file. */
    public void removeWarpFromGroup(String groupName, String warpName, Consumer<Boolean> callback) {
        repository.removeWarpEntry(groupName, warpName, callback);
    }

    public static String normalize(String name) {
        return Names.normalize(name);
    }
}
