package tr.sonumut.clan;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.gui.ConfirmMenu;
import tr.sonumut.core.ItemBuilder;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Klanlar: clans.yml. Komutlar: /klan ..., /kc <mesaj>. */
public final class ClanModule extends BaseModule implements ClanService, Listener, CommandExecutor, TabCompleter {
    private record Invite(String clan, long until) {}

    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<UUID, Clan> byMember = new ConcurrentHashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private YamlStore store;

    public ClanModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "clan"; }

    @Override public void enable() {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "clans.yml"));
        load();
        Services.register(ClanService.class, this);
        listen(this);
        plugin.bind("klan", this);
        plugin.bind("kc", this);
    }

    @Override public void disable() {
        cleanup();
        if (store != null) { save(); store.flush(); }
        clans.clear();
        byMember.clear();
        Services.unregister(ClanService.class);
        plugin.unbind("klan", "kc");
    }

    @Override public void reload() { store.load(); load(); }

    // ---------- veri ----------

    private void load() {
        clans.clear();
        byMember.clear();
        ConfigurationSection root = store.yaml().getConfigurationSection("clans");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            UUID leader;
            try { leader = UUID.fromString(s.getString("leader", "")); } catch (IllegalArgumentException e) { continue; }
            Clan c = new Clan(s.getString("tag", id), s.getString("name", id), leader);
            c.bank = s.getLong("bank", 0);
            c.created = s.getLong("created", System.currentTimeMillis());
            ConfigurationSection ms = s.getConfigurationSection("members");
            if (ms != null) {
                for (String u : ms.getKeys(false)) {
                    try {
                        UUID uid = UUID.fromString(u);
                        Clan.Role r = Clan.Role.valueOf(ms.getString(u, "UYE"));
                        c.members.put(uid, r);
                        c.joined.put(uid, 0L);
                        byMember.put(uid, c);
                    } catch (IllegalArgumentException ignored) { }
                }
            }
            if (!c.members.containsKey(leader)) { c.members.put(leader, Clan.Role.LIDER); byMember.put(leader, c); }
            clans.put(c.id, c);
        }
    }

    private void save() {
        YamlConfiguration y = store.yaml();
        y.set("clans", null);
        for (Clan c : clans.values()) {
            String b = "clans." + c.id;
            y.set(b + ".tag", c.tag);
            y.set(b + ".name", c.name);
            y.set(b + ".leader", c.leader.toString());
            y.set(b + ".bank", c.bank);
            y.set(b + ".created", c.created);
            for (Map.Entry<UUID, Clan.Role> m : c.members.entrySet()) y.set(b + ".members." + m.getKey(), m.getValue().name());
        }
        store.dirty();
    }

    @Override public Optional<Clan> of(UUID id) { return Optional.ofNullable(byMember.get(id)); }

    @Override public String tag(UUID id) {
        Clan c = byMember.get(id);
        return c == null ? "" : c.tag;
    }

    long cfgCost() { return cfg().getLong("clan.create-cost", 5000); }

    static String nameOf(UUID id) {
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "?" : n;
    }

    void tell(Clan c, String key, String def, String... kv) {
        for (UUID id : c.members.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) Lang.send(p, key, def, kv);
        }
    }

    // ---------- işlemler ----------

    boolean create(Player p, String tag, String name) {
        if (byMember.containsKey(p.getUniqueId())) { Lang.send(p, "klan.zaten", "<red>Zaten bir klandasın."); return false; }
        int min = cfg().getInt("clan.tag-min", 2), max = cfg().getInt("clan.tag-max", 4);
        if (!tag.matches("[A-Za-z0-9]{" + min + "," + max + "}")) {
            Lang.send(p, "klan.tag-gecersiz", "<red>Etiket <min>-<max> karakter, yalnızca harf ve rakam olmalı.", "min", String.valueOf(min), "max", String.valueOf(max));
            return false;
        }
        if (name.isBlank() || name.length() > 24) { Lang.send(p, "klan.ad-gecersiz", "<red>Klan adı 1-24 karakter olmalı."); return false; }
        if (clans.containsKey(tag.toLowerCase(Locale.ROOT))) { Lang.send(p, "klan.tag-kullanimda", "<red>Bu etiket kullanımda."); return false; }
        long cost = cfg().getLong("clan.create-cost", 5000);
        EconomyService eco = Services.get(EconomyService.class);
        if (eco == null || !eco.take(p.getUniqueId(), cost)) {
            Fx.fail(p);
            Lang.send(p, "klan.altin-yok", "<red>Klan kurmak için <gold><miktar> Altın <red>gerekir.", "miktar", Util.num(cost));
            return false;
        }
        Clan c = new Clan(tag, name.trim(), p.getUniqueId());
        c.members.put(p.getUniqueId(), Clan.Role.LIDER);
        clans.put(c.id, c);
        byMember.put(p.getUniqueId(), c);
        save();
        Io.log("clan", p.getName() + " klan kurdu: " + c.tag + " (" + c.name + ") ücret=" + cost);
        Fx.levelUp(p);
        Bukkit.broadcast(Lang.t("klan.kuruldu", "<gold>[<tag>] <yellow><ad> <gray>klanı <white><oyuncu> <gray>tarafından kuruldu!", "tag", c.tag, "ad", c.name, "oyuncu", p.getName()));
        return true;
    }

    void invite(Player from, Player to) {
        Clan c = byMember.get(from.getUniqueId());
        if (c == null) { Lang.send(from, "klan.yok", "<red>Bir klanda değilsin."); return; }
        if (!c.members.get(from.getUniqueId()).manages()) { Lang.send(from, "klan.yetki", "<red>Bunun için yönetici olmalısın."); return; }
        if (to.equals(from) || byMember.containsKey(to.getUniqueId())) { Lang.send(from, "klan.hedef-uygun-degil", "<red>Bu oyuncu davet edilemez."); return; }
        if (c.members.size() >= cfg().getInt("clan.max-members", 20)) { Lang.send(from, "klan.dolu", "<red>Klan dolu."); return; }
        invites.put(to.getUniqueId(), new Invite(c.id, System.currentTimeMillis() + cfg().getLong("clan.invite-seconds", 60) * 1000));
        Lang.send(from, "klan.davet-gitti", "<green><oyuncu> oyuncusuna davet gönderildi.", "oyuncu", to.getName());
        to.sendMessage(Lang.prefix().append(Lang.t("klan.davet-geldi",
                "<yellow><oyuncu> seni <gold>[<tag>] <yellow>klanına davet etti. <click:run_command:'/klan kabul'><green><bold>[Kabul Et]</bold></green></click> <click:run_command:'/klan reddet'><red>[Reddet]</red></click>",
                "oyuncu", from.getName(), "tag", c.tag)));
    }

    void accept(Player p) {
        Invite inv = invites.remove(p.getUniqueId());
        if (inv == null || inv.until() < System.currentTimeMillis()) { Lang.send(p, "klan.davet-yok", "<red>Geçerli bir davetin yok."); return; }
        Clan c = clans.get(inv.clan());
        if (c == null) { Lang.send(p, "klan.silindi", "<red>Bu klan artık yok."); return; }
        if (byMember.containsKey(p.getUniqueId())) { Lang.send(p, "klan.zaten", "<red>Zaten bir klandasın."); return; }
        if (c.members.size() >= cfg().getInt("clan.max-members", 20)) { Lang.send(p, "klan.dolu", "<red>Klan dolu."); return; }
        c.members.put(p.getUniqueId(), Clan.Role.UYE);
        byMember.put(p.getUniqueId(), c);
        save();
        Fx.ok(p);
        tell(c, "klan.katildi", "<green><oyuncu> klana katıldı!", "oyuncu", p.getName());
    }

    void leave(Player p) {
        Clan c = byMember.get(p.getUniqueId());
        if (c == null) { Lang.send(p, "klan.yok", "<red>Bir klanda değilsin."); return; }
        if (c.leader.equals(p.getUniqueId())) {
            Lang.send(p, "klan.lider-ayrilamaz", "<red>Lider ayrılamaz. Klanı silmek için <white>/klan sil <red>veya liderliği devret: <white>/klan terfi <oyuncu>");
            return;
        }
        c.members.remove(p.getUniqueId());
        byMember.remove(p.getUniqueId());
        save();
        Lang.send(p, "klan.ayrildin", "<yellow>Klandan ayrıldın.");
        tell(c, "klan.ayrildi", "<yellow><oyuncu> klandan ayrıldı.", "oyuncu", p.getName());
    }

    void kick(Player by, UUID target) {
        Clan c = byMember.get(by.getUniqueId());
        if (c == null || byMember.get(target) != c || target.equals(by.getUniqueId())) { Lang.send(by, "klan.at-hata", "<red>Bu işlemi yapamazsın."); return; }
        Clan.Role mine = c.members.get(by.getUniqueId()), theirs = c.members.get(target);
        if (!mine.manages() || theirs == Clan.Role.LIDER || (mine == Clan.Role.YONETICI && theirs != Clan.Role.UYE)) {
            Lang.send(by, "klan.at-hata", "<red>Bu işlemi yapamazsın."); return;
        }
        c.members.remove(target);
        byMember.remove(target);
        save();
        tell(c, "klan.atildi", "<yellow><oyuncu> klandan atıldı.", "oyuncu", nameOf(target));
        Player t = Bukkit.getPlayer(target);
        if (t != null) Lang.send(t, "klan.atildin", "<red>Klandan atıldın.");
    }

    /** Lider: üye -> yönetici; yönetici -> liderliği devret. */
    void promote(Player by, UUID target) {
        Clan c = byMember.get(by.getUniqueId());
        if (c == null || byMember.get(target) != c || !c.leader.equals(by.getUniqueId()) || target.equals(by.getUniqueId())) {
            Lang.send(by, "klan.at-hata", "<red>Bu işlemi yapamazsın."); return;
        }
        if (c.members.get(target) == Clan.Role.UYE) {
            c.members.put(target, Clan.Role.YONETICI);
            tell(c, "klan.terfi", "<green><oyuncu> yönetici oldu.", "oyuncu", nameOf(target));
        } else {
            c.members.put(target, Clan.Role.LIDER);
            c.members.put(by.getUniqueId(), Clan.Role.YONETICI);
            c.leader = target;
            tell(c, "klan.lider-devir", "<gold><oyuncu> yeni klan lideri!", "oyuncu", nameOf(target));
        }
        save();
    }

    void demote(Player by, UUID target) {
        Clan c = byMember.get(by.getUniqueId());
        if (c == null || byMember.get(target) != c || !c.leader.equals(by.getUniqueId()) || c.members.get(target) != Clan.Role.YONETICI) {
            Lang.send(by, "klan.at-hata", "<red>Bu işlemi yapamazsın."); return;
        }
        c.members.put(target, Clan.Role.UYE);
        save();
        tell(c, "klan.indirildi", "<yellow><oyuncu> üyeliğe indirildi.", "oyuncu", nameOf(target));
    }

    void deposit(Player p, long amount) {
        Clan c = byMember.get(p.getUniqueId());
        EconomyService eco = Services.get(EconomyService.class);
        if (c == null || eco == null) { Lang.send(p, "klan.yok", "<red>Bir klanda değilsin."); return; }
        if (amount <= 0 || !eco.take(p.getUniqueId(), amount)) { Fx.fail(p); Lang.send(p, "klan.altin-yok2", "<red>Yeterli Altının yok."); return; }
        c.bank += amount;   // önce al, sonra ver
        save();
        Io.log("clan", p.getName() + " [" + c.tag + "] bankaya yatırdı: " + amount);
        Fx.ok(p);
        tell(c, "klan.yatirdi", "<green><oyuncu> banka'ya <gold><miktar> Altın <green>yatırdı.", "oyuncu", p.getName(), "miktar", Util.num(amount));
    }

    void withdraw(Player p, long amount) {
        Clan c = byMember.get(p.getUniqueId());
        EconomyService eco = Services.get(EconomyService.class);
        if (c == null || eco == null) { Lang.send(p, "klan.yok", "<red>Bir klanda değilsin."); return; }
        if (!c.members.get(p.getUniqueId()).manages()) { Lang.send(p, "klan.yetki", "<red>Bunun için yönetici olmalısın."); return; }
        if (amount <= 0 || c.bank < amount) { Fx.fail(p); Lang.send(p, "klan.banka-yok", "<red>Klan bankasında yeterli Altın yok."); return; }
        c.bank -= amount;
        eco.give(p.getUniqueId(), amount);
        save();
        Io.log("clan", p.getName() + " [" + c.tag + "] bankadan çekti: " + amount);
        Fx.ok(p);
        tell(c, "klan.cekti", "<yellow><oyuncu> bankadan <gold><miktar> Altın <yellow>çekti.", "oyuncu", p.getName(), "miktar", Util.num(amount));
    }

    void disband(Player p) {
        Clan c = byMember.get(p.getUniqueId());
        if (c == null || !c.leader.equals(p.getUniqueId())) { Lang.send(p, "klan.yetki-lider", "<red>Yalnızca lider klanı silebilir."); return; }
        tell(c, "klan.silindi-bildir", "<red>Klan dağıtıldı.");
        EconomyService eco = Services.get(EconomyService.class);
        if (eco != null && c.bank > 0) eco.give(c.leader, c.bank);
        Io.log("clan", p.getName() + " klanı sildi: " + c.tag + " banka iade=" + c.bank);
        for (UUID u : c.members.keySet()) byMember.remove(u);
        clans.remove(c.id);
        save();
    }

    // ---------- olaylar ----------

    private static Player shooter(Entity d) {
        if (d instanceof Player p) return p;
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (cfg().getBoolean("clan.friendly-fire", false)) return;
        Player a = shooter(e.getDamager());
        if (a == null || !(e.getEntity() instanceof Player v) || a.equals(v)) return;
        Clan ca = byMember.get(a.getUniqueId());
        if (ca != null && ca == byMember.get(v.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { invites.remove(e.getPlayer().getUniqueId()); }

    // ---------- komut ----------

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
        Clan mine = byMember.get(p.getUniqueId());
        if (cmd.getName().equals("kc")) {
            if (mine == null) { Lang.send(p, "klan.yok", "<red>Bir klanda değilsin."); return true; }
            if (a.length == 0) { Lang.send(p, "klan.kc-kullanim", "<gray>Kullanım: <white>/kc <mesaj>"); return true; }
            String msg = String.join(" ", a);
            for (UUID id : mine.members.keySet()) {
                Player m = Bukkit.getPlayer(id);
                if (m != null) m.sendMessage(Lang.t("klan.sohbet", "<gold>[Klan] <yellow><oyuncu><dark_gray>: <white><mesaj>", "oyuncu", p.getName(), "mesaj", msg));
            }
            return true;
        }
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "olustur", "oluştur" -> {
                if (a.length < 3) Lang.send(p, "klan.olustur-kullanim", "<gray>Kullanım: <white>/klan olustur <etiket> <ad>");
                else create(p, a[1], String.join(" ", Arrays.copyOfRange(a, 2, a.length)));
            }
            case "davet" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) Lang.send(p, "klan.oyuncu-yok", "<red>Oyuncu bulunamadı."); else invite(p, t);
            }
            case "kabul" -> accept(p);
            case "reddet" -> { invites.remove(p.getUniqueId()); Lang.send(p, "klan.reddedildi", "<gray>Davet reddedildi."); }
            case "ayril", "ayrıl" -> leave(p);
            case "at", "terfi", "indir" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) { Lang.send(p, "klan.oyuncu-yok", "<red>Oyuncu bulunamadı."); break; }
                if (sub.equals("at")) kick(p, t.getUniqueId()); else if (sub.equals("terfi")) promote(p, t.getUniqueId()); else demote(p, t.getUniqueId());
            }
            case "yatir", "yatır" -> deposit(p, a.length > 1 ? Util.parseLong(a[1], 0) : 0);
            case "cek", "çek" -> withdraw(p, a.length > 1 ? Util.parseLong(a[1], 0) : 0);
            case "bilgi" -> {
                if (mine == null) { Lang.send(p, "klan.yok", "<red>Bir klanda değilsin."); break; }
                Lang.send(p, "klan.bilgi", "<gold>[<tag>] <yellow><ad> <gray>| Üye: <white><uye> <gray>| Banka: <gold><banka>",
                        "tag", mine.tag, "ad", mine.name, "uye", String.valueOf(mine.members.size()), "banka", Util.num(mine.bank));
            }
            case "liste" -> {
                String top = clans.values().stream().sorted(Comparator.comparingInt((Clan c) -> c.members.size()).reversed())
                        .limit(10).map(c -> "[" + c.tag + "] " + c.name + " (" + c.members.size() + ")").collect(Collectors.joining(", "));
                Lang.send(p, "klan.liste", "<gray>Klanlar: <white><liste>", "liste", top.isEmpty() ? "-" : top);
            }
            case "sil" -> {
                if (mine == null || !mine.leader.equals(p.getUniqueId())) { Lang.send(p, "klan.yetki-lider", "<red>Yalnızca lider klanı silebilir."); break; }
                new ConfirmMenu(Lang.t("gui.klan-sil", "<dark_red>Klan silinsin mi?"),
                        ItemBuilder.of(org.bukkit.Material.TNT).name(Lang.mm("<red>" + mine.name)).lore("<gray>Banka sana iade edilir.").build(),
                        this::disband, null).open(p);
            }
            default -> new ClanMenu(this).open(p);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (c.getName().equals("kc")) return out;
        if (a.length == 1) out.addAll(List.of("olustur", "davet", "kabul", "reddet", "ayril", "at", "terfi", "indir", "yatir", "cek", "bilgi", "liste", "sil", "menu"));
        else if (a.length == 2 && List.of("davet", "at", "terfi", "indir").contains(a[0].toLowerCase(Locale.ROOT)))
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
