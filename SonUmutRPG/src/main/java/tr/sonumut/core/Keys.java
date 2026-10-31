package tr.sonumut.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Tüm NamespacedKey'ler burada. */
public final class Keys {
    public static NamespacedKey MOB_LEVEL, NO_REWARD, BOSS_ID, MINION, UPGRADE, CRATE_KEY, CLASS_CARD, MEGAPHONE, HP_MOD;

    private Keys() {}

    public static void init(Plugin p) {
        MOB_LEVEL = new NamespacedKey(p, "mob_level");
        NO_REWARD = new NamespacedKey(p, "no_reward");
        BOSS_ID = new NamespacedKey(p, "boss_id");
        MINION = new NamespacedKey(p, "minion");
        UPGRADE = new NamespacedKey(p, "upgrade");
        CRATE_KEY = new NamespacedKey(p, "crate_key");
        CLASS_CARD = new NamespacedKey(p, "class_card");
        MEGAPHONE = new NamespacedKey(p, "megaphone");
        HP_MOD = new NamespacedKey(p, "hp_mod");
    }
}
