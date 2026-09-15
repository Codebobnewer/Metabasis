package com.goga221.metabasis.spawn;

import com.goga221.metabasis.location.LocationSnapshot;
import com.goga221.metabasis.util.SafeTeleport;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SpawnService {

    public enum TeleportOutcome {
        TELEPORTED_CUSTOM,
        TELEPORTED_DEFAULT,
        WORLD_NOT_FOUND,
        TELEPORT_FAILED
    }

    private final SpawnRepository repository;
    private final TaskScheduler scheduler;
    private final Map<String, Spawn> cache = new ConcurrentHashMap<>();

    public SpawnService(SpawnRepository repository, TaskScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Void> loadAllIntoCache() {
        return repository.loadAll().thenAccept(spawns -> {
            cache.clear();
            for (Spawn spawn : spawns) {
                cache.put(spawn.getWorldName(), spawn);
            }
        });
    }

    /** Case-insensitive match against currently loaded worlds — world names aren't free text like warp names. */
    public Optional<World> resolveWorld(String rawWorldName) {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getName().equalsIgnoreCase(rawWorldName))
                .findFirst();
    }

    public Optional<Spawn> get(String exactWorldName) {
        return Optional.ofNullable(cache.get(exactWorldName));
    }

    public Collection<Spawn> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public CompletableFuture<Void> setSpawn(World world, Location location, UUID setBy) {
        Spawn spawn = new Spawn(world.getName(), LocationSnapshot.from(location), setBy, System.currentTimeMillis());
        cache.put(world.getName(), spawn);
        return repository.save(spawn);
    }

    public void teleportToWorldSpawn(Player player, String rawWorldName, Consumer<TeleportOutcome> callback) {
        Optional<World> resolved = resolveWorld(rawWorldName);
        if (resolved.isEmpty()) {
            callback.accept(TeleportOutcome.WORLD_NOT_FOUND);
            return;
        }

        World world = resolved.get();
        Optional<Spawn> spawn = get(world.getName());
        Location location = spawn.map(s -> s.getLocation().toBukkitLocation(world)).orElse(world.getSpawnLocation());
        boolean usedFallback = spawn.isEmpty();

        SafeTeleport.to(player, location, scheduler, success -> callback.accept(
                !success ? TeleportOutcome.TELEPORT_FAILED
                        : usedFallback ? TeleportOutcome.TELEPORTED_DEFAULT
                        : TeleportOutcome.TELEPORTED_CUSTOM));
    }
}
