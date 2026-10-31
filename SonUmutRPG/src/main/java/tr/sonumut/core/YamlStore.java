package tr.sonumut.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import tr.sonumut.SonUmutPlugin;

import java.io.File;

/** YAML dosyası + gecikmeli, asenkron kayıt. Veri ana thread'de kopyalanır, yazım arka planda yapılır. */
public final class YamlStore {
    private final SonUmutPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private BukkitTask pending;

    public YamlStore(SonUmutPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        load();
    }

    public void load() {
        yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public YamlConfiguration yaml() { return yaml; }

    public File file() { return file; }

    public boolean exists() { return file.exists(); }

    /** Değişiklik oldu; kısa süre sonra kaydedilecek. */
    public void dirty() {
        if (pending != null) return;
        if (!plugin.isEnabled()) { flush(); return; }
        pending = Bukkit.getScheduler().runTaskLater(plugin, this::flush, 100L);
    }

    /** Hemen kaydı sıraya koyar (yazım arka planda). */
    public void flush() {
        if (pending != null) { pending.cancel(); pending = null; }
        Io.write(file, yaml.saveToString());
    }
}
