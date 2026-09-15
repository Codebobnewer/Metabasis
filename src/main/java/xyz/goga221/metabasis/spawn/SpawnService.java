package xyz.goga221.metabasis.spawn;

import xyz.goga221.metabasis.location.LocationSnapshot;
import xyz.goga221.metabasis.util.PermissionResolver;
import xyz.goga221.metabasis.util.Permissions;
import xyz.goga221.metabasis.util.SafeTeleport;
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
        NO_PERMISSION,
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
        String previousPermission = Optional.ofNullable(cache.get(world.getName())).map(Spawn::getPermission).orElse(null);
        Spawn spawn = new Spawn(world.getName(), LocationSnapshot.from(location), setBy, System.currentTimeMillis(), previousPermission);
        cache.put(world.getName(), spawn);
        return repository.save(spawn);
    }

    /**
     * Rebinds (or clears) the permission required to teleport to this world's spawn, without
     * touching its location. Fails if the world doesn't have a spawn set yet — validated the same
     * way group permissions are (op / blank / an existing LuckPerms group).
     */
    public CompletableFuture<Void> updatePermission(String exactWorldName, String rawPermission) {
        Spawn existing = cache.get(exactWorldName);
        if (existing == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "No spawn set for world '" + exactWorldName + "' yet — use /spawn set first."));
        }
        PermissionResolver.Resolution resolution = PermissionResolver.resolve(rawPermission);
        if (resolution.isRejected()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(resolution.rejectionMessage()));
        }

        Spawn updated = existing.withPermission(resolution.value());
        cache.put(exactWorldName, updated);
        return repository.save(updated);
    }

    /** Whether the player may teleport to the given world's spawn. No spawn set at all is always public. */
    public boolean canAccess(Player player, String exactWorldName) {
        return get(exactWorldName)
                .map(spawn -> spawn.getPermission() == null || Permissions.check(player, spawn.getPermission()))
                .orElse(true);
    }

    public void teleportToWorldSpawn(Player player, String rawWorldName, Consumer<TeleportOutcome> callback) {
        Optional<World> resolved = resolveWorld(rawWorldName);
        if (resolved.isEmpty()) {
            callback.accept(TeleportOutcome.WORLD_NOT_FOUND);
            return;
        }

        World world = resolved.get();
        if (!canAccess(player, world.getName())) {
            callback.accept(TeleportOutcome.NO_PERMISSION);
            return;
        }

        Optional<Spawn> spawn = get(world.getName());
        Location location = spawn.map(s -> s.getLocation().toBukkitLocation(world)).orElse(world.getSpawnLocation());
        boolean usedFallback = spawn.isEmpty();

        SafeTeleport.to(player, location, scheduler, success -> callback.accept(
                !success ? TeleportOutcome.TELEPORT_FAILED
                        : usedFallback ? TeleportOutcome.TELEPORTED_DEFAULT
                        : TeleportOutcome.TELEPORTED_CUSTOM));
    }
}
