package com.goga221.foliawarps;

import com.goga221.foliawarps.command.WarpCommand;
import com.goga221.foliawarps.warp.WarpRepository;
import com.goga221.foliawarps.warp.WarpService;
import com.goga221.foliawarps.warp.YamlWarpRepository;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaWarpsPlugin extends JavaPlugin {

    private TaskScheduler scheduler;
    private WarpService warpService;

    @Override
    public void onEnable() {
        this.scheduler = UniversalScheduler.getScheduler(this);

        WarpRepository warpRepository = new YamlWarpRepository(getDataFolder(), scheduler);
        this.warpService = new WarpService(warpRepository, scheduler);

        warpService.loadAllIntoCache().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                getSLF4JLogger().error("Failed to load warps from warps.yml", throwable);
            } else {
                getSLF4JLogger().info("Loaded {} warp(s) from warps.yml", warpService.getAll().size());
            }
        });

        new WarpCommand(warpService).register(this);
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelTasks();
        }
    }
}
