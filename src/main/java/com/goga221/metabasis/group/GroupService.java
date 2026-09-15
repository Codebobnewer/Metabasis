package com.goga221.metabasis.group;

import com.goga221.metabasis.util.Names;
import com.goga221.metabasis.util.Permissions;

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
    public static final Set<String> RESERVED_NAMES = Set.of("create", "delete", "permission", "list");

    /** The one permission value that's always valid, even without a permissions plugin — resolves via {@link org.bukkit.permissions.Permissible#isOp()}. */
    public static final String OP_ONLY_TOKEN = "op";

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

    public CompletableFuture<Void> createGroup(String rawName, String rawPermission) {
        String name = normalize(rawName);
        if (RESERVED_NAMES.contains(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("'" + name + "' is a reserved name and can't be used for a group."));
        }
        if (cache.containsKey(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("A group named '" + name + "' already exists."));
        }
        PermissionResolution resolution = resolvePermission(rawPermission);
        if (resolution.isRejected()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(resolution.rejectionMessage()));
        }

        Group group = new Group(name, resolution.value());
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
        PermissionResolution resolution = resolvePermission(rawPermission);
        if (resolution.isRejected()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(resolution.rejectionMessage()));
        }

        Group updated = existing.withPermission(resolution.value());
        cache.put(name, updated);
        return repository.save(updated);
    }

    /**
     * Validates a raw admin-supplied permission argument and resolves it to the actual Bukkit
     * permission node to store:
     * <ul>
     *   <li>blank — valid, resolves to {@code null} (public)</li>
     *   <li>{@link #OP_ONLY_TOKEN} (case-insensitive) — always valid, regardless of whether a
     *       permissions plugin is installed, since it just checks operator status</li>
     *   <li>anything else — valid only if it names an existing LuckPerms group, resolving to
     *       that group's {@code group.<name>} node (LuckPerms grants this to every member
     *       automatically, so the actual access check never needs the LuckPerms API). Without
     *       LuckPerms present, no other value is valid — a custom node could never be granted to
     *       anyone, which would silently lock every warp in the group behind an unreachable check</li>
     * </ul>
     */
    private static PermissionResolution resolvePermission(String rawPermission) {
        if (rawPermission == null || rawPermission.isBlank()) {
            return PermissionResolution.ok(null);
        }
        if (OP_ONLY_TOKEN.equalsIgnoreCase(rawPermission)) {
            return PermissionResolution.ok(OP_ONLY_TOKEN);
        }
        if (!Permissions.isLuckPermsPresent()) {
            return PermissionResolution.rejected(
                    "No permissions plugin (e.g. LuckPerms) is installed, so a custom permission can't be granted "
                            + "to anyone. Use '" + OP_ONLY_TOKEN + "' to restrict this group to operators, or leave "
                            + "the permission blank to make it public.");
        }
        if (!Permissions.isLuckPermsGroup(rawPermission)) {
            return PermissionResolution.rejected("No LuckPerms group named '" + rawPermission + "' exists.");
        }
        return PermissionResolution.ok(Permissions.luckPermsGroupNode(rawPermission));
    }

    private record PermissionResolution(String value, String rejectionMessage) {
        static PermissionResolution ok(String value) {
            return new PermissionResolution(value, null);
        }

        static PermissionResolution rejected(String message) {
            return new PermissionResolution(null, message);
        }

        boolean isRejected() {
            return rejectionMessage != null;
        }
    }

    public CompletableFuture<Boolean> deleteGroup(String rawName) {
        String name = normalize(rawName);
        cache.remove(name);
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
