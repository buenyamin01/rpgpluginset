package tr.sonumut.core;

import java.util.UUID;

public interface EconomyService {
    long gold(UUID id);
    boolean has(UUID id, long amount);
    /** Yeterli bakiye varsa düşer ve true döner. */
    boolean take(UUID id, long amount);
    void give(UUID id, long amount);

    default String fmt(long amount) { return Util.num(amount) + " Altın"; }
}
