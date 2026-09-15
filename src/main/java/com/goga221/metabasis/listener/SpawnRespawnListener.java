package com.goga221.metabasis.listener;

import com.goga221.metabasis.spawn.SpawnService;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Redirects a player's respawn to their world's custom Metabasis spawn (if one is set), in place
 * of the world's vanilla spawn point. A player's own bed or respawn anchor always takes priority
 * — this only replaces the *default* world-spawn fallback, not a player's chosen respawn point.
 */
public final class SpawnRespawnListener implements Listener {

    private final SpawnService spawnService;

    public SpawnRespawnListener(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }

        World world = event.getRespawnLocation().getWorld();
        if (world == null) {
            return;
        }

        spawnService.get(world.getName())
                .ifPresent(spawn -> event.setRespawnLocation(spawn.getLocation().toBukkitLocation(world)));
    }
}
