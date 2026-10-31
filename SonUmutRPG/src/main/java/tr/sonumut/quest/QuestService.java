package tr.sonumut.quest;

import org.bukkit.entity.Player;

import java.util.List;

public interface QuestService {
    /** Oyuncunun aktif görevleri. */
    List<QuestView> tracked(Player p);
}
