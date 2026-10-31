package tr.sonumut.core;

public enum Rarity {
    YAYGIN("Yaygın", "#AAAAAA"),
    SAGLAM("Sağlam", "#55FF55"),
    GELISMIS("Gelişmiş", "#55AAFF"),
    ESSIZ("Eşsiz", "#B05CFF"),
    EFSANEVI("Efsanevi", "#FFAA00");

    public final String ad;
    public final String hex;

    Rarity(String ad, String hex) { this.ad = ad; this.hex = hex; }

    /** MiniMessage renk açma etiketi. */
    public String tag() { return "<color:" + hex + ">"; }

    /** Renkli Türkçe ad (MiniMessage). */
    public String colored() { return tag() + ad + "</color>"; }

    /** Düşme/ağırlık yüzdesinden nadirlik. */
    public static Rarity fromChance(double pct) {
        if (pct <= 2) return EFSANEVI;
        if (pct <= 8) return ESSIZ;
        if (pct <= 20) return GELISMIS;
        if (pct <= 40) return SAGLAM;
        return YAYGIN;
    }

    public static Rarity parse(String s, Rarity def) {
        if (s == null) return def;
        try { return valueOf(s.toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return def; }
    }
}
