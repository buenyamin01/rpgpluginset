package tr.sonumut.party;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
import tr.sonumut.core.BaseModule;
import tr.sonumut.core.Fx;
import tr.sonumut.core.Lang;
import tr.sonumut.core.Services;

import java.util.*;

/** /parti ve /p: parti kurma, davet, XP paylaşımı, parti sohbeti. */
public final class PartyModule extends BaseModule implements PartyService, Listener, CommandExecutor, TabCompleter {
    private record Invite(UUID party, long until) {}

    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public PartyModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "party"; }

    @Override public void enable() {
        Services.register(PartyService.class, this);
        listen(this);
        plugin.bind("parti", this);
        plugin.bind("p", this);
    }

    @Override public void disable() {
        cleanup();
        byMember.clear();
        invites.clear();
        Services.unregister(PartyService.class);
        plugin.unbind("parti", "p");
    }

    @Override public Optional<Party> of(UUID id) { return Optional.ofNullable(byMember.get(id)); }

    @Override public Set<UUID> sharers(Player killer, Location loc) {
        Set<UUID> out = new LinkedHashSet<>();
        out.add(killer.getUniqueId());
        Party party = byMember.get(killer.getUniqueId());
        if (party == null) return out;
        double r = cfg().getDouble("party.share-radius", 50);
        for (UUID id : party.members()) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.getWorld().equals(loc.getWorld()) && m.getLocation().distanceSquared(loc) <= r * r) out.add(id);
        }
        return out;
    }

    // ---------- işlemler ----------

    void tell(Party party, String key, String def, String... kv) {
        for (UUID id : party.members()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) Lang.send(p, key, def, kv);
        }
    }

    boolean create(Player p) {
        if (byMember.containsKey(p.getUniqueId())) { Lang.send(p, "parti.zaten", "<red>Zaten bir partidesin."); return false; }
        byMember.put(p.getUniqueId(), new Party(p.getUniqueId()));
        Fx.ok(p);
        Lang.send(p, "parti.kuruldu", "<green>Parti kuruldu! <gray>Davet için: <white>/parti davet <oyuncu>");
        return true;
    }

    void invite(Player from, Player to) {
        Party party = byMember.get(from.getUniqueId());
        if (party == null) { Lang.send(from, "parti.yok", "<red>Bir partide değilsin."); return; }
        if (!party.leader().equals(from.getUniqueId())) { Lang.send(from, "parti.lider-degil", "<red>Yalnızca parti lideri davet edebilir."); return; }
        if (to.equals(from) || byMember.containsKey(to.getUniqueId())) { Lang.send(from, "parti.hedef-uygun-degil", "<red>Bu oyuncu davet edilemez."); return; }
        if (party.members().size() >= cfg().getInt("party.max-size", 5)) { Lang.send(from, "parti.dolu", "<red>Parti dolu."); return; }
        long ms = cfg().getLong("party.invite-seconds", 60) * 1000;
        invites.put(to.getUniqueId(), new Invite(party.leader(), System.currentTimeMillis() + ms));
        Lang.send(from, "parti.davet-gitti", "<green><oyuncu> oyuncusuna davet gönderildi.", "oyuncu", to.getName());
        to.sendMessage(Lang.prefix().append(Lang.t("parti.davet-geldi",
                "<yellow><oyuncu> seni partiye davet etti. <click:run_command:'/parti kabul'><green><bold>[Kabul Et]</bold></green></click> <click:run_command:'/parti reddet'><red>[Reddet]</red></click>",
                "oyuncu", from.getName())));
        Fx.sound(to, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
    }

    void accept(Player p) {
        Invite inv = invites.remove(p.getUniqueId());
        if (inv == null || inv.until() < System.currentTimeMillis()) { Lang.send(p, "parti.davet-yok", "<red>Geçerli bir davetin yok."); return; }
        if (byMember.containsKey(p.getUniqueId())) { Lang.send(p, "parti.zaten", "<red>Zaten bir partidesin."); return; }
        Party party = byMember.get(inv.party());
        if (party == null) { Lang.send(p, "parti.dagildi", "<red>Bu parti artık yok."); return; }
        if (party.members().size() >= cfg().getInt("party.max-size", 5)) { Lang.send(p, "parti.dolu", "<red>Parti dolu."); return; }
        party.members().add(p.getUniqueId());
        byMember.put(p.getUniqueId(), party);
        Fx.ok(p);
        tell(party, "parti.katildi", "<green><oyuncu> partiye katıldı.", "oyuncu", p.getName());
    }

    void leave(UUID id, boolean quit) {
        Party party = byMember.remove(id);
        if (party == null) return;
        party.members().remove(id);
        String name = nameOf(id);
        Player self = Bukkit.getPlayer(id);
        if (self != null && !quit) Lang.send(self, "parti.ayrildin", "<yellow>Partiden ayrıldın.");
        if (party.members().size() <= 1) {
            for (UUID m : new ArrayList<>(party.members())) {
                byMember.remove(m);
                Player pl = Bukkit.getPlayer(m);
                if (pl != null) Lang.send(pl, "parti.dagitildi", "<yellow>Parti dağıtıldı.");
            }
            party.members().clear();
            return;
        }
        if (party.leader().equals(id)) {
            party.setLeader(party.members().iterator().next());
            tell(party, "parti.yeni-lider", "<yellow>Yeni parti lideri: <white><oyuncu>", "oyuncu", nameOf(party.leader()));
        }
        tell(party, "parti.ayrildi", "<yellow><oyuncu> partiden ayrıldı.", "oyuncu", name);
    }

    void kick(Player leader, UUID target) {
        Party party = byMember.get(leader.getUniqueId());
        if (party == null || !party.leader().equals(leader.getUniqueId()) || !party.members().contains(target) || target.equals(leader.getUniqueId())) {
            Lang.send(leader, "parti.at-hata", "<red>Bu işlemi yapamazsın."); return;
        }
        Player t = Bukkit.getPlayer(target);
        tell(party, "parti.atildi", "<yellow><oyuncu> partiden atıldı.", "oyuncu", nameOf(target));
        leave(target, true);
        if (t != null) Lang.send(t, "parti.atildin", "<red>Partiden atıldın.");
    }

    static String nameOf(UUID id) {
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "?" : n;
    }

    // ---------- olaylar ----------

    private static Player shooter(Entity d) {
        if (d instanceof Player p) return p;
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (cfg().getBoolean("party.friendly-fire", false)) return;
        Player a = shooter(e.getDamager());
        if (a == null || !(e.getEntity() instanceof Player v) || a.equals(v)) return;
        Party pa = byMember.get(a.getUniqueId());
        if (pa != null && pa == byMember.get(v.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        invites.remove(e.getPlayer().getUniqueId());
        leave(e.getPlayer().getUniqueId(), true);
    }

    // ---------- komut ----------

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
        if (cmd.getName().equals("p")) {
            Party party = byMember.get(p.getUniqueId());
            if (party == null) { Lang.send(p, "parti.yok", "<red>Bir partide değilsin."); return true; }
            if (a.length == 0) { Lang.send(p, "parti.p-kullanim", "<gray>Kullanım: <white>/p <mesaj>"); return true; }
            String msg = String.join(" ", a);
            for (UUID id : party.members()) {
                Player m = Bukkit.getPlayer(id);
                if (m != null) m.sendMessage(Lang.t("parti.sohbet", "<dark_aqua>[Parti] <aqua><oyuncu><dark_gray>: <white><mesaj>", "oyuncu", p.getName(), "mesaj", msg));
            }
            return true;
        }
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "olustur", "oluştur" -> create(p);
            case "davet" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) Lang.send(p, "parti.oyuncu-yok", "<red>Oyuncu bulunamadı."); else invite(p, t);
            }
            case "kabul" -> accept(p);
            case "reddet" -> {
                invites.remove(p.getUniqueId());
                Lang.send(p, "parti.reddedildi", "<gray>Davet reddedildi.");
            }
            case "ayril", "ayrıl" -> {
                if (!byMember.containsKey(p.getUniqueId())) Lang.send(p, "parti.yok", "<red>Bir partide değilsin."); else leave(p.getUniqueId(), false);
            }
            case "at" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) Lang.send(p, "parti.oyuncu-yok", "<red>Oyuncu bulunamadı."); else kick(p, t.getUniqueId());
            }
            case "lider" -> {
                Party party = byMember.get(p.getUniqueId());
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (party == null || t == null || !party.leader().equals(p.getUniqueId()) || !party.members().contains(t.getUniqueId())) {
                    Lang.send(p, "parti.at-hata", "<red>Bu işlemi yapamazsın.");
                } else {
                    party.setLeader(t.getUniqueId());
                    tell(party, "parti.yeni-lider", "<yellow>Yeni parti lideri: <white><oyuncu>", "oyuncu", t.getName());
                }
            }
            case "liste" -> {
                Party party = byMember.get(p.getUniqueId());
                if (party == null) { Lang.send(p, "parti.yok", "<red>Bir partide değilsin."); break; }
                StringBuilder sb = new StringBuilder();
                for (UUID id : party.members()) sb.append(id.equals(party.leader()) ? "★" : "").append(nameOf(id)).append(", ");
                Lang.send(p, "parti.liste", "<gray>Üyeler: <white><liste>", "liste", sb.substring(0, Math.max(0, sb.length() - 2)));
            }
            default -> new PartyMenu(this).open(p);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (c.getName().equals("p")) return out;
        if (a.length == 1) out.addAll(List.of("olustur", "davet", "kabul", "reddet", "ayril", "at", "lider", "liste", "menu"));
        else if (a.length == 2 && List.of("davet", "at", "lider").contains(a[0].toLowerCase(Locale.ROOT)))
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
