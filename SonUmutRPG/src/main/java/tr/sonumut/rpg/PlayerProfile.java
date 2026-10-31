package tr.sonumut.rpg;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import tr.sonumut.core.YamlStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Oyuncunun kalıcı RPG verisi. Alanlar volatile: sohbet gibi async okumalar güvenlidir. */
public final class PlayerProfile {
    private final UUID id;
    private final YamlStore store;
    private volatile int level = 1;
    private volatile long xp;
    private volatile ClassType cls;
    private volatile int points;
    private volatile long kills;
    private volatile String name = "";
    private final Map<CombatStat, Integer> alloc = new ConcurrentHashMap<>();
    /** Kaydedilmez; çalışma anı manası. */
    public volatile double mana = -1;

    PlayerProfile(UUID id, YamlStore store) {
        this.id = id;
        this.store = store;
        YamlConfiguration y = store.yaml();
        level = Math.max(1, y.getInt("level", 1));
        xp = y.getLong("xp", 0);
        cls = ClassType.parse(y.getString("class"));
        points = y.getInt("points", 0);
        kills = y.getLong("kills", 0);
        name = y.getString("name", "");
        ConfigurationSection a = y.getConfigurationSection("alloc");
        if (a != null) {
            for (CombatStat s : CombatStat.values()) {
                int v = a.getInt(s.name(), 0);
                if (v > 0) alloc.put(s, v);
            }
        }
    }

    public UUID id() { return id; }
    public int level() { return level; }
    public long xp() { return xp; }
    public ClassType cls() { return cls; }
    public int points() { return points; }
    public long kills() { return kills; }
    public String name() { return name; }
    public int alloc(CombatStat s) { return alloc.getOrDefault(s, 0); }

    public void setLevel(int v) { level = Math.max(1, v); }
    public void setXp(long v) { xp = Math.max(0, v); }
    public void setClass(ClassType c) { cls = c; }
    public void addPoints(int d) { points = Math.max(0, points + d); }
    public void addKill() { kills++; }
    public void setName(String n) { name = n == null ? "" : n; }
    public void addAlloc(CombatStat s, int d) { alloc.merge(s, d, Integer::sum); }

    /** Diğer modüllerin kendi verisini tutması için bölüm (yoksa oluşturur). Yazdıktan sonra save() çağır. */
    public ConfigurationSection section(String path) {
        YamlConfiguration y = store.yaml();
        ConfigurationSection s = y.getConfigurationSection(path);
        return s != null ? s : y.createSection(path);
    }

    public void save() {
        YamlConfiguration y = store.yaml();
        y.set("name", name);
        y.set("level", level);
        y.set("xp", xp);
        y.set("class", cls == null ? null : cls.name());
        y.set("points", points);
        y.set("kills", kills);
        for (CombatStat s : CombatStat.values()) {
            int v = alloc(s);
            y.set("alloc." + s.name(), v == 0 ? null : v);
        }
        store.dirty();
    }

    public void flush() { save(); store.flush(); }
}
