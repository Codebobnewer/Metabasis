package xyz.goga221.metabasis.util;

/**
 * Resolves a raw admin-supplied permission argument to the value stored on a group/spawn. Shared
 * by anything that gates access behind an admin-chosen permission (groups, per-world spawns):
 * <ul>
 *   <li>blank — resolves to {@code null} (public)</li>
 *   <li>{@link #OP_ONLY_TOKEN} (case-insensitive) — resolves to {@link #OP_ONLY_TOKEN}, checked
 *       via {@link org.bukkit.permissions.Permissible#isOp()} rather than as a real node</li>
 *   <li>anything else — used as-is as a Bukkit permission node, granted however the server's
 *       permission plugin (or lack thereof) sees fit</li>
 * </ul>
 */
public final class PermissionResolver {

    /** The one permission value that's always valid, even without a permissions plugin — resolves via {@link org.bukkit.permissions.Permissible#isOp()}. */
    public static final String OP_ONLY_TOKEN = "op";

    private PermissionResolver() {
    }

    /** The resolved value to store: {@code null} (public), {@link #OP_ONLY_TOKEN}, or the raw permission node. */
    public static String resolve(String rawPermission) {
        if (rawPermission == null || rawPermission.isBlank()) {
            return null;
        }
        if (OP_ONLY_TOKEN.equalsIgnoreCase(rawPermission)) {
            return OP_ONLY_TOKEN;
        }
        return rawPermission;
    }
}
