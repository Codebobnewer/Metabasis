package com.goga221.metabasis.warp;

import com.goga221.metabasis.location.YamlLocationCodec;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
    public void save(Warp warp, Consumer<Boolean> callback) {
        runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection section = config.createSection(ROOT_KEY + "." + warp.getName());
            YamlLocationCodec.write(section, warp.getLocation());
            section.set("creator", warp.getCreator().toString());
            section.set("created-at", warp.getCreatedAt());
            if (warp.getGroupName() != null) {
                section.set("group", warp.getGroupName());
            }
            config.save(warpsFile);
            return true;
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void delete(String name, Consumer<Boolean> callback) {
        runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection warps = config.getConfigurationSection(ROOT_KEY);
            boolean existed = warps != null && warps.isConfigurationSection(name);
            if (existed) {
                warps.set(name, null);
                config.save(warpsFile);
            }
            return existed;
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void loadAll(Consumer<List<Warp>> onLoaded, Consumer<Throwable> onError) {
        runAsync(() -> {
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
                            YamlLocationCodec.read(warpSection),
                            UUID.fromString(warpSection.getString("creator")),
                            warpSection.getLong("created-at"),
                            warpSection.getString("group")
                    ));
                }
            }
            return warps;
        }, onLoaded, onError);
    }

    /** Runs a YAML read/write action asynchronously, serialized against concurrent file access. */
    private <T> void runAsync(IOAction<T> action, Consumer<T> onDone, Consumer<Throwable> onError) {
        scheduler.runTaskAsynchronously(() -> {
            synchronized (lock) {
                try {
                    onDone.accept(action.run());
                } catch (IOException e) {
                    onError.accept(e);
                }
            }
        });
    }

    @FunctionalInterface
    private interface IOAction<T> {
        T run() throws IOException;
    }
}
