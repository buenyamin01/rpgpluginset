package tr.sonumut.rpg;

import org.bukkit.Material;

import java.util.Locale;

public enum ClassType {
    //            ad          renk1      renk2      ikon  malzeme                 açıklama                                 +can  yetenek            mana cd   KS  KH  CC  KAC YAN MY   GAN XP
    SAVASCI("Savaşçı", "#ff6b6b", "#c0392b", "⚔", Material.IRON_SWORD, "Ağır darbeler ve yüksek dayanıklılık.", 6, "Yer Sarsıntısı", 30, 8, 5, 50, 3, 2, 8, 1.0, 0, 0),
    OKCU("Okçu", "#7bed9f", "#2ed573", "➶", Material.BOW, "Uzaktan isabetli ve şanslı avcı.", 0, "Ok Yağmuru", 25, 6, 12, 60, 0, 8, 0, 1.0, 5, 0),
    BUYUCU("Büyücü", "#74b9ff", "#a29bfe", "✦", Material.BLAZE_ROD, "Bol mana, yıkıcı büyüler.", 0, "Ateş Topu", 20, 3, 6, 50, 0, 3, 0, 3.0, 0, 5),
    SUIKASTCI("Suikastçı", "#b2bec3", "#636e72", "☠", Material.NETHERITE_SWORD, "Hızlı, ölümcül ve kaçamak.", -2, "Gölge Adımı", 20, 5, 15, 80, 0, 12, 0, 1.5, 3, 0),
    SIFACI("Şifacı", "#ffeaa7", "#fdcb6e", "✚", Material.GOLDEN_APPLE, "Takımı iyileştirir, zor ölür.", 4, "Şifa Dalgası", 30, 10, 4, 40, 6, 4, 3, 2.0, 0, 8);

    public final String ad, renk1, renk2, ikon, aciklama, yetenek;
    public final Material icon;
    public final double hpBonus;
    public final int manaCost, cooldown;
    private final double[] stats;

    ClassType(String ad, String renk1, String renk2, String ikon, Material icon, String aciklama, double hpBonus,
              String yetenek, int manaCost, int cooldown, double... stats) {
        this.ad = ad;
        this.renk1 = renk1;
        this.renk2 = renk2;
        this.ikon = ikon;
        this.icon = icon;
        this.aciklama = aciklama;
        this.hpBonus = hpBonus;
        this.yetenek = yetenek;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.stats = stats;
    }

    public double base(CombatStat s) { return s.ordinal() < stats.length ? stats[s.ordinal()] : 0; }

    /** MiniMessage gradyanlı ad. */
    public String gradient() { return "<gradient:" + renk1 + ":" + renk2 + ">" + ad + "</gradient>"; }

    public static ClassType parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
}
