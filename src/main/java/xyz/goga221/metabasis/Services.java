package xyz.goga221.metabasis;

import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.gui.GuiContext;
import xyz.goga221.metabasis.history.WarpHistoryRepository;
import xyz.goga221.metabasis.spawn.SpawnService;
import xyz.goga221.metabasis.util.MessageService;
import xyz.goga221.metabasis.warp.WarpService;
import lombok.Getter;

/** Static accessor for plugin services. Command classes are instantiated by reflection with a no-arg constructor, so they can't take these via constructor injection. */
public final class Services {

    // Lombok's class-level @Getter only covers instance fields, so each static field needs its own @Getter.
    @Getter
    private static WarpService warpService;
    @Getter
    private static GroupService groupService;
    @Getter
    private static SpawnService spawnService;
    @Getter
    private static MessageService messageService;
    @Getter
    private static WarpHistoryRepository historyRepository;
    @Getter
    private static GuiContext guiContext;

    private Services() {
    }

    public static void init(WarpService warpService, GroupService groupService, SpawnService spawnService,
                             MessageService messageService, WarpHistoryRepository historyRepository, GuiContext guiContext) {
        Services.warpService = warpService;
        Services.groupService = groupService;
        Services.spawnService = spawnService;
        Services.messageService = messageService;
        Services.historyRepository = historyRepository;
        Services.guiContext = guiContext;
    }
}
