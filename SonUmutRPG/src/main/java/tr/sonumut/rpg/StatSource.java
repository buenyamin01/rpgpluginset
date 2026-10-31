package tr.sonumut.rpg;

import org.bukkit.entity.Player;

/** Başka modüllerin (ör. eşyalar, klan) savaş istatistiklerine katkı vermesi için. */
@FunctionalInterface
public interface StatSource {
    double bonus(Player p, CombatStat stat);
}
