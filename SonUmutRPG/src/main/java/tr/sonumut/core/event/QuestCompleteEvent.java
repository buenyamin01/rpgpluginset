package tr.sonumut.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

/** Ana thread'de çağrılır. */
public class QuestCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String questId;

    public QuestCompleteEvent(Player player, String questId) {
        this.player = player;
        this.questId = questId;
    }

    public Player getPlayer() { return player; }
    public String getQuestId() { return questId; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
