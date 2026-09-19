package xyz.goga221.metabasis.group;

import xyz.goga221.metabasis.location.YamlLocationCodec;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final Logger logger;
    private final Object lock = new Object();

    /**
     * Each group's file, lazily parsed once and reused for every subsequent read/write — this
     * repository is the sole writer of these files, so re-parsing one from disk before every
     * single save would be wasted I/O; only the final {@code config.save(...)} needs disk.
     * Access only ever happens from within {@link #lock}, so a plain map is safe here.
     */
    private final Map<String, YamlConfiguration> configs = new HashMap<>();

    public YamlGroupRepository(File dataFolder, TaskScheduler scheduler, Logger logger) {
        this.groupsDirectory = new File(dataFolder, "groups");
        if (!groupsDirectory.exists() && !groupsDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create groups folder: " + groupsDirectory);
        }
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> save(Group group) {
        return runAsync(() -> {
            YamlConfiguration cfg = configurationFor(group.getName());
            cfg.set("id", group.getName());
            cfg.set("permission", group.getPermission());
            cfg.set("description", group.getDescription());
            cfg.set("enabled", group.isEnabled());
            cfg.save(fileFor(group.getName()));
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
            configs.remove(name);
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
            YamlConfiguration cfg = configurationFor(groupName);
            ConfigurationSection section = cfg.createSection(WARPS_KEY + "." + warp.getName());
            YamlLocationCodec.write(section, warp.getLocation());
            section.set("creator", warp.getCreator().toString());
            section.set("created-at", warp.getCreatedAt());
            section.set("enabled", warp.isEnabled());
            section.set("fade-in-ticks", warp.getFadeInTicks());
            section.set("stay-ticks", warp.getStayTicks());
            section.set("fade-out-ticks", warp.getFadeOutTicks());
            section.set("warmup-seconds", warp.getWarmupSeconds());
            cfg.save(fileFor(groupName));
            return true;
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void removeWarpEntry(String groupName, String warpName, Consumer<Boolean> callback) {
        runAsync(() -> {
            YamlConfiguration cfg = configurationFor(groupName);
            ConfigurationSection warps = cfg.getConfigurationSection(WARPS_KEY);
            boolean existed = warps != null && warps.isConfigurationSection(warpName);
            if (existed) {
                warps.set(warpName, null);
                cfg.save(fileFor(groupName));
            }
            return existed;
        }, callback, throwable -> callback.accept(false));
    }

    private GroupData readGroupData(File file) throws IOException {
        String name = stripExtension(file.getName());
        YamlConfiguration config = configurationFor(name);
        String id = config.getString("id", name);
        Group group = new Group(id, config.getString("permission"), config.getString("description"), config.getBoolean("enabled", true));

        List<GroupedWarp> warps = new ArrayList<>();
        ConfigurationSection warpsSection = config.getConfigurationSection(WARPS_KEY);
        if (warpsSection != null) {
            for (String warpName : warpsSection.getKeys(false)) {
                ConfigurationSection warpSection = warpsSection.getConfigurationSection(warpName);
                if (warpSection == null) {
                    continue;
                }
                try {
                    warps.add(new GroupedWarp(
                            warpName,
                            YamlLocationCodec.read(warpSection),
                            UUID.fromString(warpSection.getString("creator")),
                            warpSection.getLong("created-at"),
                            warpSection.getBoolean("enabled", true),
                            warpSection.getInt("fade-in-ticks", 0),
                            warpSection.getInt("stay-ticks", 0),
                            warpSection.getInt("fade-out-ticks", 0),
                            warpSection.getInt("warmup-seconds", 0)
                    ));
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Skipping corrupt warp entry '" + warpName + "' in group '" + name + "'", e);
                }
            }
        }
        return new GroupData(group, warps);
    }

    /** Called only from within {@link #lock}, so lazy init needs no extra synchronization. */
    private YamlConfiguration configurationFor(String groupName) {
        return configs.computeIfAbsent(groupName, name -> YamlConfiguration.loadConfiguration(fileFor(name)));
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
                    logger.log(Level.SEVERE, "Group file read/write failed", e);
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
