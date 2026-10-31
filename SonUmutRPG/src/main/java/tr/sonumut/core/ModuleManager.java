package tr.sonumut.core;

import tr.sonumut.SonUmutPlugin;

import java.util.*;

public final class ModuleManager {
    private final SonUmutPlugin plugin;
    private final Map<String, Module> all = new LinkedHashMap<>();
    private final Set<String> active = new LinkedHashSet<>();

    public ModuleManager(SonUmutPlugin plugin) { this.plugin = plugin; }

    public void add(Module m) { all.put(m.id(), m); }

    public Set<String> ids() { return all.keySet(); }

    public boolean isActive(String id) { return active.contains(id); }

    public void enableAll() {
        for (Module m : all.values()) {
            if (plugin.getConfig().getBoolean("modules." + m.id(), true)) enable(m.id());
        }
    }

    public boolean enable(String id) {
        Module m = all.get(id);
        if (m == null || active.contains(id)) return false;
        try {
            m.enable();
            active.add(id);
            plugin.getLogger().info("Modül açıldı: " + id);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Modül açılamadı: " + id, t);
            try { m.disable(); } catch (Throwable ignored) { }
            return false;
        }
    }

    public boolean disable(String id) {
        Module m = all.get(id);
        if (m == null || !active.contains(id)) return false;
        active.remove(id);
        try { m.disable(); } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Modül kapatılırken hata: " + id, t);
        }
        return true;
    }

    public void disableAll() {
        List<String> rev = new ArrayList<>(active);
        Collections.reverse(rev);
        for (String id : rev) disable(id);
    }

    public void reloadAll() {
        for (String id : new ArrayList<>(active)) {
            try { all.get(id).reload(); } catch (Throwable t) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Modül yenilenemedi: " + id, t);
            }
        }
    }
}
