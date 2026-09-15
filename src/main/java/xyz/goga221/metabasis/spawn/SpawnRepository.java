package xyz.goga221.metabasis.spawn;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SpawnRepository {

    CompletableFuture<Void> save(Spawn spawn);

    CompletableFuture<List<Spawn>> loadAll();
}
