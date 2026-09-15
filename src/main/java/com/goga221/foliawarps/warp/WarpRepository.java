package com.goga221.foliawarps.warp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WarpRepository {

    CompletableFuture<Void> save(Warp warp);

    CompletableFuture<Boolean> delete(String name);

    CompletableFuture<List<Warp>> loadAll();
}
