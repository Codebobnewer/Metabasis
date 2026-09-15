package xyz.goga221.metabasis.util;

/**
 * Validates a raw admin-supplied permission argument and resolves it to the actual Bukkit
 * permission node to store. Shared by anything that gates access behind an admin-chosen
 * permission (groups, per-world spawns):
 * <ul>
 *   <li>blank — valid, resolves to {@code null} (public)</li>
 *   <li>{@link #OP_ONLY_TOKEN} (case-insensitive) — always valid, regardless of whether a
 *       permissions plugin is installed, since it just checks operator status</li>
 *   <li>anything else — valid only if it names an existing LuckPerms group, resolving to that
 *       group's {@code group.<name>} node (LuckPerms grants this to every member automatically,
 *       so the actual access check never needs the LuckPerms API). Without LuckPerms present, no
 *       other value is valid — a custom node could never be granted to anyone, which would
 *       silently lock access behind an unreachable check</li>
 * </ul>
 */
public final class PermissionResolver {

    /** The one permission value that's always valid, even without a permissions plugin — resolves via {@link org.bukkit.permissions.Permissible#isOp()}. */
    public static final String OP_ONLY_TOKEN = "op";

    private PermissionResolver() {
    }

    public static Resolution resolve(String rawPermission) {
        if (rawPermission == null || rawPermission.isBlank()) {
            return Resolution.ok(null);
        }
        if (OP_ONLY_TOKEN.equalsIgnoreCase(rawPermission)) {
            return Resolution.ok(OP_ONLY_TOKEN);
        }
        if (!Permissions.isLuckPermsPresent()) {
            return Resolution.rejected(
                    "No permissions plugin (e.g. LuckPerms) is installed, so a custom permission can't be granted "
                            + "to anyone. Use '" + OP_ONLY_TOKEN + "' to restrict this to operators, or leave "
                            + "the permission blank to make it public.");
        }
        if (!Permissions.isLuckPermsGroup(rawPermission)) {
            return Resolution.rejected("No LuckPerms group named '" + rawPermission + "' exists.");
        }
        return Resolution.ok(Permissions.luckPermsGroupNode(rawPermission));
    }

    public record Resolution(String value, String rejectionMessage) {
        public static Resolution ok(String value) {
            return new Resolution(value, null);
        }

        public static Resolution rejected(String message) {
            return new Resolution(null, message);
        }

        public boolean isRejected() {
            return rejectionMessage != null;
        }
    }
}
