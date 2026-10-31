package tr.sonumut.core;

import tr.sonumut.SonUmutPlugin;

import java.io.File;
import java.util.UUID;

/** Eklentinin kendi Altın ekonomisi. data/economy.yml */
public final class EconomyImpl implements EconomyService {
    private static final long CAP = Long.MAX_VALUE / 4;
    private final YamlStore store;

    public EconomyImpl(SonUmutPlugin plugin) {
        this.store = new YamlStore(plugin, new File(plugin.getDataFolder(), "data/economy.yml"));
    }

    @Override public long gold(UUID id) { return store.yaml().getLong("g." + id, 0L); }

    @Override public boolean has(UUID id, long amount) { return amount >= 0 && gold(id) >= amount; }

    @Override public boolean take(UUID id, long amount) {
        if (amount < 0) return false;
        long cur = gold(id);
        if (cur < amount) return false;
        store.yaml().set("g." + id, cur - amount);
        store.dirty();
        return true;
    }

    @Override public void give(UUID id, long amount) {
        if (amount <= 0) return;
        long cur = gold(id);
        store.yaml().set("g." + id, Math.min(CAP, cur + amount));
        store.dirty();
    }

    public void flush() { store.flush(); }
}
