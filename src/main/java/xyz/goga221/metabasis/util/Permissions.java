package xyz.goga221.metabasis.util;

import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Permissions {

    public static final String ADMIN = "metabasis.admin";

    /** LuckPerms grants this node (lowercase group name) to every member of a group automatically. */
    private static final String GROUP_NODE_PREFIX = "group.";

    private Permissions() {
    }

    /**
     * Whether the given permissible has the given node, treating server operators as always
     * authorized. This guards against permission plugins (e.g. LuckPerms with
     * {@code apply-bukkit-default-permissions} disabled) that don't honor plugin.yml's
     * {@code default: op} declarations.
     */
    public static boolean check(Permissible permissible, String node) {
        return permissible.isOp() || permissible.hasPermission(node);
    }

    /** Whether the LuckPerms plugin is installed and enabled. Checked by plugin name, not the API, so this is cheap and safe to call unconditionally. */
    public static boolean isLuckPermsPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    /** Names of every LuckPerms group currently configured on the server, or empty if LuckPerms isn't present. */
    public static Set<String> luckPermsGroupNames() {
        if (!isLuckPermsPresent()) {
            return Set.of();
        }
        return LuckPermsProvider.get().getGroupManager().getLoadedGroups().stream()
                .map(net.luckperms.api.model.group.Group::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Whether the given name matches an existing LuckPerms group (case-insensitive; LuckPerms group names are always lowercase internally). */
    public static boolean isLuckPermsGroup(String name) {
        return luckPermsGroupNames().contains(name.toLowerCase(Locale.ROOT));
    }

    /** The Bukkit permission node LuckPerms automatically grants to every member of the given group. */
    public static String luckPermsGroupNode(String groupName) {
        return GROUP_NODE_PREFIX + groupName.toLowerCase(Locale.ROOT);
    }
}
