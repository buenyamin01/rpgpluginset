package tr.sonumut.mob;

import org.bukkit.entity.LivingEntity;

public interface MobLevelService {
    /** Mobun seviyesi; seviyelenmemişse 1. */
    int levelOf(LivingEntity mob);

    /** Mobu verilen seviyeye göre ölçekler (bir kez çağır) ve adını günceller. */
    void apply(LivingEntity mob, int level);
}
