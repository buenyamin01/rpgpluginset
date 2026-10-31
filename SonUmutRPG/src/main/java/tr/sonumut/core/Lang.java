package tr.sonumut.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import tr.sonumut.SonUmutPlugin;

import java.io.File;

/**
 * Tüm oyuncu metinleri messages.yml'den gelir. Varsayılan metin çağrı yerinde verilir;
 * dosyada anahtar yoksa varsayılan kullanılır ve dosyaya yazılır.
 * Yer tutucular: kv = {"oyuncu", "Ali", "miktar", "5"} -> <oyuncu>, <miktar>
 */
public final class Lang {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX_DEF = "<dark_gray>[<gradient:#ffb347:#ffe27a>SonUmut</gradient><dark_gray>] <gray>";
    private static YamlStore store;

    private Lang() {}

    public static void init(SonUmutPlugin plugin) {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "messages.yml"));
    }

    public static void reload() { store.load(); }

    public static void flush() { if (store != null) store.flush(); }

    public static String raw(String key, String def) {
        YamlConfiguration y = store.yaml();
        String v = y.getString(key);
        if (v == null) {
            y.set(key, def);
            store.dirty();
            return def;
        }
        return v;
    }

    private static TagResolver[] resolvers(String[] kv) {
        int n = kv.length / 2;
        TagResolver[] r = new TagResolver[n];
        for (int i = 0; i < n; i++) r[i] = Placeholder.unparsed(kv[i * 2], kv[i * 2 + 1]);
        return r;
    }

    /** messages.yml'den bileşen. */
    public static Component t(String key, String def, String... kv) {
        return MM.deserialize(raw(key, def), resolvers(kv));
    }

    /** Doğrudan MiniMessage metni (dosyadan değil). */
    public static Component mm(String s, String... kv) { return MM.deserialize(s, resolvers(kv)); }

    public static Component prefix() { return MM.deserialize(raw("genel.onek", PREFIX_DEF)); }

    /** Önek + mesaj gönderir. */
    public static void send(CommandSender to, String key, String def, String... kv) {
        to.sendMessage(prefix().append(t(key, def, kv)));
    }

    public static String plain(Component c) { return PlainTextComponentSerializer.plainText().serialize(c); }
}
