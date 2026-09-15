package xyz.goga221.metabasis.history;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * SQLite-backed (via HikariCP) implementation, storing to {@code history.db} in the plugin's
 * data folder. Scoped narrowly to warp history/usage — warps/groups/spawns themselves stay in
 * YAML, since that data is small, config-like, and benefits from being human-editable; a growing,
 * append-only, queryable log is exactly what a database is for.
 */
public final class SqliteWarpHistoryRepository implements WarpHistoryRepository, AutoCloseable {

    private final HikariDataSource dataSource;
    private final TaskScheduler scheduler;

    public SqliteWarpHistoryRepository(File dataFolder, TaskScheduler scheduler) {
        this.scheduler = scheduler;

        File databaseFile = new File(dataFolder, "history.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        // SQLite only supports one writer at a time; a single pooled connection gives the same
        // serialization the YAML repositories get from their explicit lock, without needing one.
        config.setMaximumPoolSize(1);
        config.setPoolName("Metabasis-History");
        this.dataSource = new HikariDataSource(config);

        createSchema();
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS warp_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        warp_name TEXT NOT NULL,
                        event_type TEXT NOT NULL,
                        actor TEXT,
                        timestamp INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS warp_usage (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        warp_name TEXT NOT NULL,
                        player TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_warp_events_name ON warp_events(warp_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_warp_usage_name ON warp_usage(warp_name)");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the warp history database", e);
        }
    }

    @Override
    public void recordEvent(String warpName, String eventType, UUID actor, Consumer<Boolean> callback) {
        runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO warp_events (warp_name, event_type, actor, timestamp) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, warpName);
                statement.setString(2, eventType);
                statement.setString(3, actor == null ? null : actor.toString());
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
                return true;
            }
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void recordUsage(String warpName, UUID player, Consumer<Boolean> callback) {
        runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO warp_usage (warp_name, player, timestamp) VALUES (?, ?, ?)")) {
                statement.setString(1, warpName);
                statement.setString(2, player.toString());
                statement.setLong(3, System.currentTimeMillis());
                statement.executeUpdate();
                return true;
            }
        }, callback, throwable -> callback.accept(false));
    }

    @Override
    public void getRecentEvents(String warpName, int limit, Consumer<List<WarpHistoryEntry>> onLoaded, Consumer<Throwable> onError) {
        runAsync(() -> {
            List<WarpHistoryEntry> entries = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT warp_name, event_type, actor, timestamp FROM warp_events "
                                 + "WHERE warp_name = ? ORDER BY timestamp DESC LIMIT ?")) {
                statement.setString(1, warpName);
                statement.setInt(2, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String actorString = resultSet.getString("actor");
                        entries.add(new WarpHistoryEntry(
                                resultSet.getString("warp_name"),
                                resultSet.getString("event_type"),
                                actorString == null ? null : UUID.fromString(actorString),
                                resultSet.getLong("timestamp")
                        ));
                    }
                }
                return entries;
            }
        }, onLoaded, onError);
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private <T> void runAsync(SqlAction<T> action, Consumer<T> onDone, Consumer<Throwable> onError) {
        scheduler.runTaskAsynchronously(() -> {
            try {
                onDone.accept(action.run());
            } catch (SQLException e) {
                onError.accept(e);
            }
        });
    }

    @FunctionalInterface
    private interface SqlAction<T> {
        T run() throws SQLException;
    }
}
