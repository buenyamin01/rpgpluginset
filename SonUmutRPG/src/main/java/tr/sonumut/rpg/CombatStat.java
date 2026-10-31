package tr.sonumut.rpg;

import org.bukkit.Material;

public enum CombatStat {
    KRITIK_SANS("Kritik Şansı", "%", 0.5, 80, Material.GOLDEN_SWORD),
    KRITIK_HASAR("Kritik Hasarı", "%", 2.0, 300, Material.NETHERITE_SWORD),
    CAN_CALMA("Can Çalma", "%", 0.3, 25, Material.GLISTERING_MELON_SLICE),
    KACINMA("Kaçınma", "%", 0.4, 50, Material.FEATHER),
    YANSIMA("Hasar Yansıtma", "%", 0.5, 40, Material.SHIELD),
    MANA_YENILEME("Mana Yenileme", "/sn", 0.1, 10, Material.LAPIS_LAZULI),
    GANIMET_SANSI("Ganimet Şansı", "%", 0.5, 100, Material.RABBIT_FOOT),
    XP_BONUS("XP Bonusu", "%", 0.5, 100, Material.EXPERIENCE_BOTTLE);

    public final String ad;
    public final String birim;
    /** Her yetenek puanının getirdiği değer. */
    public final double perPoint;
    public final double cap;
    public final Material icon;

    CombatStat(String ad, String birim, double perPoint, double cap, Material icon) {
        this.ad = ad;
        this.birim = birim;
        this.perPoint = perPoint;
        this.cap = cap;
        this.icon = icon;
    }
}
