package tr.sonumut.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Modüllerin birbirini yalnızca arayüzlerle tanıması için basit servis kaydı. */
public final class Services {
    private static final Map<Class<?>, Object> MAP = new ConcurrentHashMap<>();

    private Services() {}

    public static <T> void register(Class<T> type, T impl) { MAP.put(type, impl); }

    /** Servis yoksa null döner. */
    public static <T> T get(Class<T> type) { return type.cast(MAP.get(type)); }

    public static void unregister(Class<?> type) { MAP.remove(type); }

    public static void clear() { MAP.clear(); }
}
