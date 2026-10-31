package tr.sonumut.rpg;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface ProfileService {
    /** Ana thread'de kullan. Yüklü değilse diskten yükler. */
    PlayerProfile get(Player p);
    PlayerProfile get(UUID id);
    /** Yalnızca yüklü profili verir (diske gitmez); async okumalar için güvenli. Yoksa null. */
    PlayerProfile peek(UUID id);

    int level(Player p);
    ClassType classOf(Player p);
    void addXp(Player p, long amount, String kaynak);
    double combatStat(Player p, CombatStat stat);
    void registerStatSource(StatSource source);

    double mana(Player p);
    double maxMana(Player p);
    boolean useMana(Player p, double cost);
    long xpNeeded(int level);
    void setClass(Player p, ClassType type);
    void applyAttributes(Player p);
}
