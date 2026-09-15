package com.goga221.foliawarps.warp;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class YamlWarpRepository implements WarpRepository {

    private static final String ROOT_KEY = "warps";

    private final File warpsFile;
    private final TaskScheduler scheduler;
    private final Object lock = new Object();

    public YamlWarpRepository(File dataFolder, TaskScheduler scheduler) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder: " + dataFolder);
        }
        this.warpsFile = new File(dataFolder, "warps.yml");
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Void> save(Warp warp) {
        return runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection section = config.createSection(ROOT_KEY + "." + warp.getName());
            section.set("world", warp.getWorldName());
            section.set("x", warp.getX());
            section.set("y", warp.getY());
            section.set("z", warp.getZ());
            section.set("yaw", warp.getYaw());
            section.set("pitch", warp.getPitch());
            section.set("creator", warp.getCreator().toString());
            section.set("created-at", warp.getCreatedAt());
            config.save(warpsFile);
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(String name) {
        return runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection warps = config.getConfigurationSection(ROOT_KEY);
            boolean existed = warps != null && warps.isConfigurationSection(name);
            if (existed) {
                warps.set(name, null);
                config.save(warpsFile);
            }
            return existed;
        });
    }

    @Override
    public CompletableFuture<List<Warp>> loadAll() {
        return runAsync(() -> {
            List<Warp> warps = new ArrayList<>();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection section = config.getConfigurationSection(ROOT_KEY);
            if (section != null) {
                for (String name : section.getKeys(false)) {
                    ConfigurationSection warpSection = section.getConfigurationSection(name);
                    if (warpSection == null) {
                        continue;
                    }
                    warps.add(new Warp(
                            name,
                            warpSection.getString("world"),
                            warpSection.getDouble("x"),
                            warpSection.getDouble("y"),
                            warpSection.getDouble("z"),
                            (float) warpSection.getDouble("yaw"),
                            (float) warpSection.getDouble("pitch"),
                            UUID.fromString(warpSection.getString("creator")),
                            warpSection.getLong("created-at")
                    ));
                }
            }
            return warps;
        });
    }

    /** Runs a YAML read/write action asynchronously, serialized against concurrent file access. */
    private <T> CompletableFuture<T> runAsync(IOAction<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runTaskAsynchronously(() -> {
            synchronized (lock) {
                try {
                    future.complete(action.run());
                } catch (IOException e) {
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
