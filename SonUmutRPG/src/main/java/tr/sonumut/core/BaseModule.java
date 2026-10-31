package tr.sonumut.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import tr.sonumut.SonUmutPlugin;

import java.util.ArrayList;
import java.util.List;

/** Dinleyici ve zamanlayıcıları toplayıp disable'da temizleyen yardımcı taban sınıf. */
public abstract class BaseModule implements Module {
    protected final SonUmutPlugin plugin;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    protected BaseModule(SonUmutPlugin plugin) { this.plugin = plugin; }

    protected FileConfiguration cfg() { return plugin.getConfig(); }

    protected void listen(Listener l) {
        Bukkit.getPluginManager().registerEvents(l, plugin);
        listeners.add(l);
    }

    protected BukkitTask every(long delay, long period, Runnable r) {
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, r, delay, period);
        tasks.add(t);
        return t;
    }

    protected void cleanup() {
        for (Listener l : listeners) HandlerList.unregisterAll(l);
        listeners.clear();
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
    }
}
