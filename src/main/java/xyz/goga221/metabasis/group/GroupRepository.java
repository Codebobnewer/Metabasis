package xyz.goga221.metabasis.group;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface GroupRepository {

    CompletableFuture<Void> save(Group group);

    /** Deletes the group's entire file, including any warps embedded in it. */
    CompletableFuture<Boolean> delete(String name);

    /** Loads every group together with the warps embedded in its file. */
    CompletableFuture<List<GroupData>> loadAll();

    /**
     * Adds or updates one warp entry inside the given group's file.
     * <p>Callback-based (not {@link CompletableFuture}) because this is called from
     * {@code WarpService}, whose public API deliberately avoids that type.
     */
    void saveWarpEntry(String groupName, GroupedWarp warp, Consumer<Boolean> callback);

    /** Removes one warp entry from the given group's file. Callback-based for the same reason as {@link #saveWarpEntry}. */
    void removeWarpEntry(String groupName, String warpName, Consumer<Boolean> callback);
}
