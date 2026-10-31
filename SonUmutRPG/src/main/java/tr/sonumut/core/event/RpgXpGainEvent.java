package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

/** Ana thread'de çağrılır. */
public class RpgXpGainEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final long amount;
    private final String source;

    public RpgXpGainEvent(Player player, long amount, String source) {
        this.player = player;
        this.amount = amount;
        this.source = source;
    }

    public Player getPlayer() { return player; }
    public long getAmount() { return amount; }
    public String getSource() { return source; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
