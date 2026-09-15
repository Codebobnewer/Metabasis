package xyz.goga221.metabasis.api;

import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.warp.Warp;
import xyz.goga221.metabasis.warp.WarpService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public final class MetabasisAPIImpl implements MetabasisAPI {

    private final WarpService warpService;
    private final GroupService groupService;

    public MetabasisAPIImpl(WarpService warpService, GroupService groupService) {
        this.warpService = warpService;
        this.groupService = groupService;
    }

    @Override
    public Optional<Warp> getWarp(String name) {
        return warpService.get(name);
    }

    @Override
    public Collection<Warp> getAllWarps() {
        return warpService.getAll();
    }

    @Override
    public Optional<Group> getGroup(String name) {
        return groupService.get(name);
    }

    @Override
    public Collection<Group> getAllGroups() {
        return groupService.getAll();
    }

    @Override
    public boolean canAccess(Player player, Warp warp) {
        return warpService.canAccess(player, warp);
    }

    @Override
    public void teleportToWarp(Player player, String warpName, boolean override, Consumer<Boolean> callback) {
        Optional<Warp> warp = warpService.get(warpName);
        if (warp.isEmpty()) {
            callback.accept(false);
            return;
        }

        Warp target = warp.get();
        if (!override && !warpService.canAccess(player, target)) {
            callback.accept(false);
            return;
        }

        warpService.teleport(player, target, callback);
    }
}
