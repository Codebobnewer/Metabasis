package xyz.goga221.metabasis.api.event;

import xyz.goga221.metabasis.group.Group;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a group is deleted (its warps are already gone too, per the cascading-delete behavior). */
public final class GroupDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Group group;

    public GroupDeleteEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
