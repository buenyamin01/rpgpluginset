package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Ana thread'de çağrılır. */
public class ItemUpgradeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack item;
    private final int newPlus;
    private final boolean success;

    public ItemUpgradeEvent(Player player, ItemStack item, int newPlus, boolean success) {
        this.player = player;
        this.item = item;
        this.newPlus = newPlus;
        this.success = success;
    }

    public Player getPlayer() { return player; }
    public ItemStack getItem() { return item; }
    public int getNewPlus() { return newPlus; }
    public boolean getSuccess() { return success; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
