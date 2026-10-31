package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

/** Ana thread'de çağrılır. */
public class RpgLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int oldLevel;
    private final int newLevel;

    public RpgLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() { return player; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
