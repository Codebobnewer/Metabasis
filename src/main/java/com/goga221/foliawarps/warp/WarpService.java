package com.goga221.foliawarps.warp;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class WarpService {

    /** Names that collide with /warp's own subcommand literals and can't be used for a warp. */
    public static final Set<String> RESERVED_NAMES = Set.of("set", "del", "list");

    private final WarpRepository repository;
    private final TaskScheduler scheduler;
    private final Map<String, Warp> cache = new ConcurrentHashMap<>();

    public WarpService(WarpRepository repository, TaskScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Void> loadAllIntoCache() {
        return repository.loadAll().thenAccept(warps -> {
            cache.clear();
            for (Warp warp : warps) {
                cache.put(warp.getName(), warp);
            }
        });
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

    public CompletableFuture<Void> createWarp(String rawName, Location location, UUID creator) {
        String name = normalize(rawName);
        if (RESERVED_NAMES.contains(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("'" + name + "' is a reserved name and can't be used for a warp."));
        }

        World world = location.getWorld();
        if (world == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Location has no world"));
        }

        Warp warp = new Warp(
                name,
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                creator,
                System.currentTimeMillis()
        );

        cache.put(name, warp);
        return repository.save(warp);
    }

    public CompletableFuture<Boolean> deleteWarp(String rawName) {
        String name = normalize(rawName);
        cache.remove(name);
        return repository.delete(name);
    }

    public void teleport(Player player, Warp warp, Consumer<Boolean> callback) {
        World world = Bukkit.getWorld(warp.getWorldName());
        if (world == null) {
            callback.accept(false);
            return;
        }

        Location location = new Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());

        // Entity#teleportAsync is the Folia/Paper-safe way to cross region/world boundaries;
        // the callback may run off the player's thread, so hop back before touching the player again.
        player.teleportAsync(location).thenAccept(success ->
                scheduler.runTask(player, () -> callback.accept(success)));
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
