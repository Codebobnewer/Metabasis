package com.goga221.metabasis.group;

import com.goga221.metabasis.location.YamlLocationCodec;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Stores each group as its own file under {@code groups/<name>.yml}, holding the group's
 * permission plus every warp currently assigned to it (a group's file is the source of truth
 * for its warps' data — {@code WarpService} moves a warp's entry between this and warps.yml
 * whenever its group assignment changes).
 */
public final class YamlGroupRepository implements GroupRepository {

    private static final String WARPS_KEY = "warps";

    private final File groupsDirectory;
    private final TaskScheduler scheduler;
    private final Object lock = new Object();

    public YamlGroupRepository(File dataFolder, TaskScheduler scheduler) {
        this.groupsDirectory = new File(dataFolder, "groups");
        if (!groupsDirectory.exists() && !groupsDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create groups folder: " + groupsDirectory);
        }
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Void> save(Group group) {
        return runAsync(() -> {
            File file = fileFor(group.getName());
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("id", group.getName());
            config.set("permission", group.getPermission());
            config.save(file);
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(String name) {
        return runAsync(() -> {
            File file = fileFor(name);
            boolean existed = file.exists();
            if (existed && !file.delete()) {
                throw new IOException("Could not delete group file: " + file);
            }
            return existed;
        });
    }

    @Override
    public CompletableFuture<List<GroupData>> loadAll() {
        return runAsync(() -> {
            List<GroupData> result = new ArrayList<>();
            File[] files = groupsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    result.add(readGroupData(file));
                }
            }
            return result;
        });
    }

    @Override
    public void saveWarpEntry(String groupName, GroupedWarp warp, Consumer<Boolean> callback) {
        runAsync(() -> {
            File file = fileFor(groupName);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = config.createSection(WARPS_KEY + "." + warp.getName());
            YamlLocationCodec.write(section, warp.getLocation());
            section.set("creator", warp.getCreator().toString());
            section.set("created-at", warp.getCreatedAt());
            config.save(file);
            return true;
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void removeWarpEntry(String groupName, String warpName, Consumer<Boolean> callback) {
        runAsync(() -> {
            File file = fileFor(groupName);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection warps = config.getConfigurationSection(WARPS_KEY);
            boolean existed = warps != null && warps.isConfigurationSection(warpName);
            if (existed) {
                warps.set(warpName, null);
                config.save(file);
            }
            return existed;
        }, callback, throwable -> callback.accept(false));
    }

    private GroupData readGroupData(File file) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("id", stripExtension(file.getName()));
        Group group = new Group(id, config.getString("permission"));

        List<GroupedWarp> warps = new ArrayList<>();
        ConfigurationSection warpsSection = config.getConfigurationSection(WARPS_KEY);
        if (warpsSection != null) {
            for (String warpName : warpsSection.getKeys(false)) {
                ConfigurationSection warpSection = warpsSection.getConfigurationSection(warpName);
                if (warpSection == null) {
                    continue;
                }
                warps.add(new GroupedWarp(
                        warpName,
                        YamlLocationCodec.read(warpSection),
                        UUID.fromString(warpSection.getString("creator")),
                        warpSection.getLong("created-at")
                ));
            }
        }
        return new GroupData(group, warps);
    }

    private File fileFor(String groupName) {
        return new File(groupsDirectory, groupName + ".yml");
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".yml".length());
    }

    private <T> CompletableFuture<T> runAsync(IOAction<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsync(action, future::complete, future::completeExceptionally);
        return future;
    }

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
