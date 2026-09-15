package xyz.goga221.metabasis.warp;

import java.util.List;
import java.util.function.Consumer;

public interface WarpRepository {

    /** Saves the warp, then reports whether the write succeeded. */
    void save(Warp warp, Consumer<Boolean> callback);

    /** Deletes the warp, then reports whether it existed and was removed. */
    void delete(String name, Consumer<Boolean> callback);

    void loadAll(Consumer<List<Warp>> onLoaded, Consumer<Throwable> onError);
}
