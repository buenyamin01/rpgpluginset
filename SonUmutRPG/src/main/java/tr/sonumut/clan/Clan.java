package tr.sonumut.clan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Clan {
    public enum Role {
        LIDER("Lider"), YONETICI("Yönetici"), UYE("Üye");
        public final String ad;
        Role(String ad) { this.ad = ad; }
        public boolean manages() { return this != UYE; }
    }

    final String id;       // küçük harfli etiket (benzersiz)
    final String tag;      // görünen etiket (BÜYÜK)
    String name;
    UUID leader;
    long bank;
    long created;
    final Map<UUID, Role> members = new ConcurrentHashMap<>();
    /** Sıralı üye listesi gösterimi için ekleme sırası. */
    final Map<UUID, Long> joined = new LinkedHashMap<>();

    Clan(String tag, String name, UUID leader) {
        this.id = tag.toLowerCase(java.util.Locale.ROOT);
        this.tag = tag.toUpperCase(java.util.Locale.ROOT);
        this.name = name;
        this.leader = leader;
        this.created = System.currentTimeMillis();
    }

    public String id() { return id; }
    public String tag() { return tag; }
    public String name() { return name; }
    public UUID leader() { return leader; }
    public long bank() { return bank; }
    public Map<UUID, Role> members() { return members; }
    public Role roleOf(UUID u) { return members.get(u); }
}
