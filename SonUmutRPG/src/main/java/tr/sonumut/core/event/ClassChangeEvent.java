package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import tr.sonumut.rpg.ClassType;

/** Ana thread'de çağrılır. */
public class ClassChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ClassType oldClass;
    private final ClassType newClass;

    public ClassChangeEvent(Player player, ClassType oldClass, ClassType newClass) {
        this.player = player;
        this.oldClass = oldClass;
        this.newClass = newClass;
    }

    public Player getPlayer() { return player; }
    public ClassType getOldClass() { return oldClass; }
    public ClassType getNewClass() { return newClass; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
