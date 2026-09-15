package xyz.goga221.metabasis.util;

import java.util.Map;

/**
 * Built-in fallback text for every message key, used whenever {@code message.yml} doesn't define
 * one (a fresh install before first extraction, or a key added in an update the admin's existing
 * file predates). This is the single source of truth for what ships in the bundled
 * {@code src/main/resources/message.yml} — keep the two in sync when adding a key.
 */
final class MessageDefaults {

    static final Map<String, String> ALL = Map.ofEntries(
            Map.entry("error.generic", "<red><message></red>"),

            Map.entry("warp.not-found", "<red>No warp named <white><name></white> exists.</red>"),
            Map.entry("warp.no-permission", "<red>You don't have permission to warp to <white><name></white>.</red>"),
            Map.entry("warp.set.success", "<green>Warp <white><name></white> created.</green>"),
            Map.entry("warp.set.failed", "<red>Failed to save warp <white><name></white>.</red>"),
            Map.entry("warp.delete.success", "<green>Warp <white><name></white> deleted.</green>"),
            Map.entry("warp.group.cleared", "<green>Warp <white><name></white> is now public.</green>"),
            Map.entry("warp.group.assigned", "<green>Warp <white><name></white> assigned to group <white><group></white>.</green>"),
            Map.entry("warp.update.failed", "<red>Failed to update warp <white><name></white>.</red>"),
            Map.entry("warp.enable.success", "<green>Warp <white><name></white> enabled.</green>"),
            Map.entry("warp.disable.success", "<yellow>Warp <white><name></white> disabled.</yellow>"),
            Map.entry("warp.massport.summary", "<green>Mass-warped <white><teleported></white>/<white><total></white> player(s) to <white><name></white> (<white><skipped></white> skipped due to missing permission).</green>"),
            Map.entry("warp.list.empty", "<yellow>There are no warps yet.</yellow>"),
            Map.entry("warp.list.header", "<gold>Warps:</gold> "),
            Map.entry("warp.teleport.success", "<green>Teleported to <white><name></white>.</green>"),
            Map.entry("warp.teleport.failed", "<red>Teleportation to <white><name></white> failed. The world may not be loaded.</red>"),
            Map.entry("warp.fade.title", "<gold><name></gold>"),
            Map.entry("warp.fade.subtitle", ""),
            Map.entry("warp.fade.success", "<green>Fade settings for <white><name></white> updated.</green>"),
            Map.entry("warp.warmup.starting", "<yellow>Warping to <white><name></white> in <white><seconds></white> second(s)... don't move!</yellow>"),
            Map.entry("warp.warmup.cancelled", "<red>Warp cancelled — you moved or took damage.</red>"),
            Map.entry("warp.warmup.set.success", "<green>Warmup for <white><name></white> set to <white><seconds></white> second(s).</green>"),
            Map.entry("warp.history.empty", "<yellow>No recorded history for <white><name></white>.</yellow>"),
            Map.entry("warp.history.header", "<gold>History for <white><name></white>:</gold>"),
            Map.entry("warp.history.entry", "<gray>[<white><time></white>] <white><type></white> by <white><actor></white></gray>"),
            Map.entry("warp.history.failed", "<red>Failed to load history for <white><name></white>.</red>"),

            Map.entry("group.not-found", "<red>No group named <white><name></white> exists.</red>"),
            Map.entry("group.create.success", "<green>Group <white><name></white> created.</green>"),
            Map.entry("group.create.failed", "<red>Failed to create group <white><name></white>.</red>"),
            Map.entry("group.delete.success", "<green>Group <white><name></white> deleted, along with every warp assigned to it.</green>"),
            Map.entry("group.permission.success", "<green>Group <white><name></white> permission updated.</green>"),
            Map.entry("group.describe.success", "<green>Group <white><name></white> description updated.</green>"),
            Map.entry("group.enable.success", "<green>Group <white><name></white> enabled.</green>"),
            Map.entry("group.disable.success", "<yellow>Group <white><name></white> disabled; its warps are inaccessible until re-enabled.</yellow>"),
            Map.entry("group.update.failed", "<red>Failed to update group <white><name></white>.</red>"),
            Map.entry("group.list.empty", "<yellow>There are no groups yet.</yellow>"),
            Map.entry("group.list.header", "<gold>Groups:</gold> "),

            Map.entry("spawn.world-not-found", "<red>No world named <white><name></white> is loaded.</red>"),
            Map.entry("spawn.no-permission", "<red>You don't have permission to warp to <white><name></white>'s spawn.</red>"),
            Map.entry("spawn.set.success", "<green>Spawn for <white><name></white> set to your current location.</green>"),
            Map.entry("spawn.set.failed", "<red>Failed to save spawn for <white><name></white>.</red>"),
            Map.entry("spawn.permission.success", "<green>Spawn permission for <white><name></white> updated.</green>"),
            Map.entry("spawn.permission.failed", "<red>Failed to update spawn permission for <white><name></white>.</red>"),
            Map.entry("spawn.teleport.custom", "<green>Teleported to <white><name></white>'s spawn.</green>"),
            Map.entry("spawn.teleport.default", "<green>Teleported to <white><name></white>'s default spawn (no custom spawn set).</green>"),
            Map.entry("spawn.teleport.failed", "<red>Teleportation to <white><name></white>'s spawn failed.</red>")
    );

    private MessageDefaults() {
    }
}
