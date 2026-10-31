package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.UUID;

/** Ana thread'de çağrılır. */
public class BossDefeatEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String bossId;
    private final Map<UUID,Double> damage;

    public BossDefeatEvent(String bossId, Map<UUID,Double> damage) {
        this.bossId = bossId;
        this.damage = damage;
    }

    public String getBossId() { return bossId; }
    public Map<UUID,Double> getDamage() { return damage; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
