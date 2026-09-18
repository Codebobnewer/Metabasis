package xyz.goga221.metabasis.command.spawn;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.spawn.Spawn;
import xyz.goga221.metabasis.util.PermissionResolver;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Objects;
import java.util.stream.Stream;

/** Suggestion providers shared by the spawn leaf commands and the root {@code /spawn} command. */
public final class SpawnCommandSupport {

    private SpawnCommandSupport() {
    }

    public static String[] worldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .toArray(String[]::new);
    }

    /** "op" plus every permission node already bound to another spawn, so a staff member can reuse one instead of retyping it. */
    public static String[] permissionSuggestions() {
        return Stream.concat(Stream.of(PermissionResolver.OP_ONLY_TOKEN),
                        Services.getSpawnService().getAll().stream().map(Spawn::getPermission).filter(Objects::nonNull).distinct())
                .toArray(String[]::new);
    }
}
