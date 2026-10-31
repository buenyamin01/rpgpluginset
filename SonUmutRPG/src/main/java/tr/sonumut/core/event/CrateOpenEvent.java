package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Ana thread'de çağrılır. */
public class CrateOpenEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String crateId;
    private final ItemStack reward;
    private final long gold;

    public CrateOpenEvent(Player player, String crateId, ItemStack reward, long gold) {
        this.player = player;
        this.crateId = crateId;
        this.reward = reward;
        this.gold = gold;
    }

    public Player getPlayer() { return player; }
    public String getCrateId() { return crateId; }
    public ItemStack getReward() { return reward; }
    public long getGold() { return gold; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
