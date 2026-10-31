package tr.sonumut.trade;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;

import java.util.*;

/** /takas <oyuncu>, /takas kabul|red|altin <miktar>. Güvenli takas: iki taraf onaylar, 3 sn geri sayım, değişiklik onayı sıfırlar. */
public final class TradeModule extends BaseModule implements Listener, CommandExecutor, TabCompleter {
    private record Request(UUID from, long until) {}

    private final Map<UUID, Request> requests = new HashMap<>();
    final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "trade"; }

    @Override public void enable() {
        listen(this);
        plugin.bind("takas", this);
    }

    @Override public void disable() {
        for (TradeSession s : new ArrayList<>(sessions.values())) s.cancel(null);
        cleanup();
        requests.clear();
        sessions.clear();
        plugin.unbind("takas");
    }

    SonUmutPlugin plugin() { return plugin; }

    void start(Player a, Player b) {
        if (sessions.containsKey(a.getUniqueId()) || sessions.containsKey(b.getUniqueId())) {
            Lang.send(a, "takas.mesgul", "<red>Taraflardan biri zaten bir takasta.");
            return;
        }
        TradeSession s = new TradeSession(this, a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
        s.open();
    }

    private void request(Player from, Player to) {
        if (from.equals(to)) { Lang.send(from, "takas.kendin", "<red>Kendinle takas yapamazsın."); return; }
        double max = cfg().getDouble("trade.max-distance", 0);
        if (max > 0 && (!from.getWorld().equals(to.getWorld()) || from.getLocation().distance(to.getLocation()) > max)) {
            Lang.send(from, "takas.uzak", "<red>Oyuncu çok uzakta."); return;
        }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(to.getUniqueId())) { Lang.send(from, "takas.mesgul", "<red>Taraflardan biri zaten bir takasta."); return; }
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), System.currentTimeMillis() + cfg().getLong("trade.request-seconds", 30) * 1000));
        Lang.send(from, "takas.istek-gitti", "<green><oyuncu> oyuncusuna takas isteği gönderildi.", "oyuncu", to.getName());
        to.sendMessage(Lang.prefix().append(Lang.t("takas.istek-geldi",
                "<yellow><oyuncu> seninle takas yapmak istiyor. <click:run_command:'/takas kabul'><green><bold>[Kabul Et]</bold></green></click> <click:run_command:'/takas red'><red>[Reddet]</red></click>",
                "oyuncu", from.getName())));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
        if (a.length == 0) { Lang.send(p, "takas.kullanim", "<gray>Kullanım: <white>/takas <oyuncu> <gray>| <white>kabul <gray>| <white>red <gray>| <white>altin <miktar>"); return true; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "kabul" -> {
                Request r = requests.remove(p.getUniqueId());
                Player from = r == null ? null : Bukkit.getPlayer(r.from());
                if (r == null || r.until() < System.currentTimeMillis() || from == null) { Lang.send(p, "takas.istek-yok", "<red>Geçerli bir takas isteğin yok."); return true; }
                start(from, p);
            }
            case "red" -> {
                Request r = requests.remove(p.getUniqueId());
                if (r != null) {
                    Player from = Bukkit.getPlayer(r.from());
                    if (from != null) Lang.send(from, "takas.reddedildi", "<red><oyuncu> takas isteğini reddetti.", "oyuncu", p.getName());
                }
                Lang.send(p, "takas.red-tamam", "<gray>İstek reddedildi.");
            }
            case "altin", "altın" -> {
                TradeSession ts = sessions.get(p.getUniqueId());
                if (ts == null) { Lang.send(p, "takas.yok", "<red>Aktif bir takasın yok."); return true; }
                long n = a.length > 1 ? Util.parseLong(a[1], -1) : -1;
                if (n < 0) { Lang.send(p, "takas.altin-kullanim", "<gray>Kullanım: <white>/takas altin <miktar>"); return true; }
                ts.setGold(p, n);
            }
            default -> {
                Player t = Bukkit.getPlayerExact(a[0]);
                if (t == null) Lang.send(p, "takas.oyuncu-yok", "<red>Oyuncu bulunamadı."); else request(p, t);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) { out.addAll(List.of("kabul", "red", "altin")); Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName())); }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        requests.remove(e.getPlayer().getUniqueId());
        TradeSession s = sessions.get(e.getPlayer().getUniqueId());
        if (s != null) s.cancel(e.getPlayer());
    }
}
