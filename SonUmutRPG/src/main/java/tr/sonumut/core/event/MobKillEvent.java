package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

/** Ana thread'de çağrılır. */
public class MobKillEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player killer;
    private final LivingEntity mob;
    private final int mobLevel;

    public MobKillEvent(Player killer, LivingEntity mob, int mobLevel) {
        this.killer = killer;
        this.mob = mob;
        this.mobLevel = mobLevel;
    }

    public Player getKiller() { return killer; }
    public LivingEntity getMob() { return mob; }
    public int getMobLevel() { return mobLevel; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
