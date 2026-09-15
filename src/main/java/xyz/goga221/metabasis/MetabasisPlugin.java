package xyz.goga221.metabasis;

import xyz.goga221.metabasis.api.MetabasisAPI;
import xyz.goga221.metabasis.api.MetabasisAPIImpl;
import xyz.goga221.metabasis.command.GroupCommand;
import xyz.goga221.metabasis.command.SpawnCommand;
import xyz.goga221.metabasis.command.WarpCommand;
import xyz.goga221.metabasis.group.GroupRepository;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.group.YamlGroupRepository;
import xyz.goga221.metabasis.gui.GuiContext;
import xyz.goga221.metabasis.history.SqliteWarpHistoryRepository;
import xyz.goga221.metabasis.listener.SpawnRespawnListener;
import xyz.goga221.metabasis.listener.WarmupCancelListener;
import xyz.goga221.metabasis.listener.WarpHistoryListener;
import xyz.goga221.metabasis.spawn.SpawnRepository;
import xyz.goga221.metabasis.spawn.SpawnService;
import xyz.goga221.metabasis.spawn.YamlSpawnRepository;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.WarpRepository;
import xyz.goga221.metabasis.warp.WarpService;
import xyz.goga221.metabasis.warp.YamlWarpRepository;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class MetabasisPlugin extends JavaPlugin {

    private TaskScheduler scheduler;
    private GroupService groupService;
    private WarpService warpService;
    private SpawnService spawnService;
    private MessageService messageService;
    private SqliteWarpHistoryRepository historyRepository;

    @Override
    public void onEnable() {
        // Ships as a bundled resource rather than being created ad hoc — first-run extraction,
        // not overwriting anything an admin has already edited.
        saveResource("message.yml", false);
        this.messageService = new MessageService(getDataFolder(), getLogger());

        this.scheduler = UniversalScheduler.getScheduler(this);

        GroupRepository groupRepository = new YamlGroupRepository(getDataFolder(), scheduler);
        this.groupService = new GroupService(groupRepository);

        WarpRepository warpRepository = new YamlWarpRepository(getDataFolder(), scheduler);
        this.warpService = new WarpService(warpRepository, groupService, scheduler, messageService);

        this.historyRepository = new SqliteWarpHistoryRepository(getDataFolder(), scheduler);
        getServer().getPluginManager().registerEvents(new WarpHistoryListener(historyRepository), this);

        // Lets other plugins fetch Bukkit.getServicesManager().load(MetabasisAPI.class) instead
        // of reaching into WarpService/GroupService directly — a stable surface that survives
        // internal refactors.
        getServer().getServicesManager().register(MetabasisAPI.class,
                new MetabasisAPIImpl(warpService, groupService), this, ServicePriority.Normal);

        // Groups must finish loading before warps: a grouped warp's data lives only in its
        // group's file, so WarpService needs that data to seed its own cache correctly.
        groupService.loadAllIntoCache().whenComplete((groupData, throwable) -> {
            if (throwable != null) {
                getLogger().log(Level.SEVERE, "Failed to load groups from the groups/ folder", throwable);
                return;
            }
            getLogger().info("Loaded " + groupService.getAll().size() + " group(s) from the groups/ folder");

            warpService.loadAllIntoCache(groupData,
                    () -> getLogger().info("Loaded " + warpService.getAll().size() + " warp(s)"),
                    warpThrowable -> getLogger().log(Level.SEVERE, "Failed to load warps from warps.yml", warpThrowable));
        });

        SpawnRepository spawnRepository = new YamlSpawnRepository(getDataFolder(), scheduler);
        this.spawnService = new SpawnService(spawnRepository, scheduler);
        spawnService.loadAllIntoCache().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                getLogger().log(Level.SEVERE, "Failed to load spawns from spawns.yml", throwable);
            } else {
                getLogger().info("Loaded " + spawnService.getAll().size() + " spawn(s) from spawns.yml");
            }
        });

        GuiContext guiContext = new GuiContext(warpService, groupService, spawnService, messageService, scheduler);

        new WarpCommand(warpService, groupService, historyRepository, messageService, guiContext).register(this);
        new SpawnCommand(spawnService, messageService).register(this);
        new GroupCommand(groupService, warpService, messageService).register(this);

        getServer().getPluginManager().registerEvents(new SpawnRespawnListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new WarmupCancelListener(warpService, messageService), this);
    }

    @Override
    public void onDisable() {
        if (historyRepository != null) {
            historyRepository.close();
        }
        if (scheduler != null) {
            scheduler.cancelTasks();
        }
    }
}
