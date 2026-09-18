package xyz.goga221.metabasis.util;

import org.bukkit.permissions.Permissible;

public final class Permissions {

    public static final String ADMIN = "metabasis.admin";

    private Permissions() {
    }

    /**
     * Whether the given permissible has the given node, treating server operators as always
     * authorized. This guards against permission plugins that don't honor plugin.yml's
     * {@code default: op} declarations.
     */
    public static boolean check(Permissible permissible, String node) {
        return permissible.isOp() || permissible.hasPermission(node);
    }
}
