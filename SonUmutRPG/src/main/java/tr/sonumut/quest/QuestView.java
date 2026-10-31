package tr.sonumut.quest;

/** Oyuncunun aktif bir görevinin özeti (HUD vb. için). */
public record QuestView(String id, String name, String desc, int progress, int goal, long gold, long xp) {}
