package xyz.goga221.metabasis.spawn;

import xyz.goga221.metabasis.location.YamlLocationCodec;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class YamlSpawnRepository implements SpawnRepository {

    private static final String ROOT_KEY = "spawns";

    private final File spawnsFile;
    private final TaskScheduler scheduler;
    private final Logger logger;
    private final Object lock = new Object();

    /**
     * Lazily loaded once, then reused for every subsequent read/write — this repository is the
     * sole writer of spawns.yml, so re-parsing it from disk before every single save would be
     * wasted I/O; only the final {@code config.save(...)} actually needs to touch disk.
     */
    private YamlConfiguration config;

    public YamlSpawnRepository(File dataFolder, TaskScheduler scheduler, Logger logger) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder: " + dataFolder);
        }
        this.spawnsFile = new File(dataFolder, "spawns.yml");
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> save(Spawn spawn) {
        return runAsync(() -> {
            YamlConfiguration cfg = configuration();
            ConfigurationSection section = cfg.createSection(ROOT_KEY + "." + spawn.getWorldName());
            YamlLocationCodec.write(section, spawn.getLocation());
            section.set("set-by", spawn.getSetBy().toString());
            section.set("updated-at", spawn.getUpdatedAt());
            section.set("permission", spawn.getPermission());
            cfg.save(spawnsFile);
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Spawn>> loadAll() {
        return runAsync(() -> {
            List<Spawn> spawns = new ArrayList<>();
            YamlConfiguration cfg = configuration();
            ConfigurationSection section = cfg.getConfigurationSection(ROOT_KEY);
            if (section != null) {
                for (String worldName : section.getKeys(false)) {
                    ConfigurationSection spawnSection = section.getConfigurationSection(worldName);
                    if (spawnSection == null) {
                        continue;
                    }
                    try {
                        spawns.add(new Spawn(
                                worldName,
                                YamlLocationCodec.read(spawnSection),
                                UUID.fromString(spawnSection.getString("set-by")),
                                spawnSection.getLong("updated-at"),
                                spawnSection.getString("permission")
                        ));
                    } catch (RuntimeException e) {
                        logger.log(Level.WARNING, "Skipping corrupt spawn entry for world '" + worldName + "' in spawns.yml", e);
                    }
                }
            }
            return spawns;
        });
    }

    /** Called only from within the {@link #lock}, so lazy init needs no extra synchronization. */
    private YamlConfiguration configuration() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(spawnsFile);
        }
        return config;
    }

    private <T> CompletableFuture<T> runAsync(IOAction<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runTaskAsynchronously(() -> {
            synchronized (lock) {
                try {
                    future.complete(action.run());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "spawns.yml read/write failed", e);
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    @FunctionalInterface
    private interface IOAction<T> {
        T run() throws IOException;
    }
}
