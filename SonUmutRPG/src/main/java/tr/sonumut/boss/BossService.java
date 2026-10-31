package tr.sonumut.boss;

import org.bukkit.entity.Entity;

public interface BossService {
    /** Varlık bir boss ise kimliği, değilse null. */
    String bossId(Entity e);

    default boolean isBoss(Entity e) { return bossId(e) != null; }
}
