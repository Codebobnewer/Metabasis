package xyz.goga221.metabasis.command.group;

import xyz.goga221.metabasis.Services;
import xyz.goga221.metabasis.group.Group;
import xyz.goga221.metabasis.group.GroupService;
import xyz.goga221.metabasis.util.Messages;
import xyz.goga221.metabasis.util.PermissionResolver;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/** Suggestion providers and the shared create/update completion handler used by more than one leaf command in this package. */
final class GroupCommandSupport {

    private GroupCommandSupport() {
    }

    static String[] groupNames() {
        return Services.getGroupService().getAll().stream()
                .map(Group::getName)
                .toArray(String[]::new);
    }

    /** "op" plus every permission node already bound to another group, so a staff member can reuse one instead of retyping it. */
    static String[] permissionSuggestions() {
        GroupService groupService = Services.getGroupService();
        return Stream.concat(Stream.of(PermissionResolver.OP_ONLY_TOKEN),
                        groupService.getAll().stream().map(Group::getPermission).filter(Objects::nonNull).distinct())
                .toArray(String[]::new);
    }

    /** Sends the message for {@code successKey} on completion, or the group's validation message (or {@code failureKey}) on failure. */
    static void handleUpdate(Player player, CompletableFuture<Void> operation, String name, String successKey, String failureKey) {
        operation
                .thenRun(() -> Services.getMessageService().send(player, successKey, Messages.name(name)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof IllegalArgumentException) {
                        Services.getMessageService().send(player, "error.generic", Messages.message(cause.getMessage()));
                    } else {
                        Services.getMessageService().send(player, failureKey, Messages.name(name));
                    }
                    return null;
                });
    }
}
