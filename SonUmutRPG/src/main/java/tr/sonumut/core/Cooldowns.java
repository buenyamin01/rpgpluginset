package tr.sonumut.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {
    private static final Map<String, Long> MAP = new ConcurrentHashMap<>();

    private Cooldowns() {}

    private static String k(UUID id, String key) { return id + ":" + key; }

    /** Kalan süre (ms), hazırsa 0. */
    public static long left(UUID id, String key) {
        Long until = MAP.get(k(id, key));
        if (until == null) return 0;
        long l = until - System.currentTimeMillis();
        if (l <= 0) { MAP.remove(k(id, key)); return 0; }
        return l;
    }

    public static boolean ready(UUID id, String key) { return left(id, key) == 0; }

    public static void set(UUID id, String key, long ms) { MAP.put(k(id, key), System.currentTimeMillis() + ms); }

    /** Hazırsa süreyi başlatır ve true döner; değilse false. */
    public static boolean tryUse(UUID id, String key, long ms) {
        if (!ready(id, key)) return false;
        set(id, key, ms);
        return true;
    }

    public static void clear(UUID id) { MAP.keySet().removeIf(s -> s.startsWith(id + ":")); }
}
