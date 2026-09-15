package com.goga221.metabasis;

import com.goga221.metabasis.command.GroupCommand;
import com.goga221.metabasis.command.SpawnCommand;
import com.goga221.metabasis.command.WarpCommand;
import com.goga221.metabasis.group.GroupRepository;
import com.goga221.metabasis.group.GroupService;
import com.goga221.metabasis.group.YamlGroupRepository;
import com.goga221.metabasis.spawn.SpawnRepository;
import com.goga221.metabasis.spawn.SpawnService;
import com.goga221.metabasis.spawn.YamlSpawnRepository;
import com.goga221.metabasis.warp.WarpRepository;
import com.goga221.metabasis.warp.WarpService;
import com.goga221.metabasis.warp.YamlWarpRepository;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class MetabasisPlugin extends JavaPlugin {

    private TaskScheduler scheduler;
    private GroupService groupService;
    private WarpService warpService;
    private SpawnService spawnService;

    @Override
    public void onEnable() {
        this.scheduler = UniversalScheduler.getScheduler(this);

        GroupRepository groupRepository = new YamlGroupRepository(getDataFolder(), scheduler);
        this.groupService = new GroupService(groupRepository);

        WarpRepository warpRepository = new YamlWarpRepository(getDataFolder(), scheduler);
        this.warpService = new WarpService(warpRepository, groupService, scheduler);

        // Groups must finish loading before warps: a grouped warp's data lives only in its
        // group's file, so WarpService needs that data to seed its own cache correctly.
        groupService.loadAllIntoCache().whenComplete((groupData, throwable) -> {
            if (throwable != null) {
                getSLF4JLogger().error("Failed to load groups from the groups/ folder", throwable);
                return;
            }
            getSLF4JLogger().info("Loaded {} group(s) from the groups/ folder", groupService.getAll().size());

            warpService.loadAllIntoCache(groupData,
                    () -> getSLF4JLogger().info("Loaded {} warp(s)", warpService.getAll().size()),
                    warpThrowable -> getSLF4JLogger().error("Failed to load warps from warps.yml", warpThrowable));
        });

        SpawnRepository spawnRepository = new YamlSpawnRepository(getDataFolder(), scheduler);
        this.spawnService = new SpawnService(spawnRepository, scheduler);
        spawnService.loadAllIntoCache().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                getSLF4JLogger().error("Failed to load spawns from spawns.yml", throwable);
            } else {
                getSLF4JLogger().info("Loaded {} spawn(s) from spawns.yml", spawnService.getAll().size());
            }
        });

        new WarpCommand(warpService, groupService).register(this);
        new SpawnCommand(spawnService).register(this);
        new GroupCommand(groupService, warpService).register(this);
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelTasks();
        }
    }
}
