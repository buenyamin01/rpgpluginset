package tr.sonumut.quest;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.core.event.BossDefeatEvent;
import tr.sonumut.core.event.QuestCompleteEvent;
import tr.sonumut.rpg.PlayerProfile;
import tr.sonumut.rpg.ProfileService;

import java.io.File;
import java.time.Duration;
import java.util.*;

/** Görevler: quests.yml. Tür: KILL (mob), MINE (blok), BOSS. Hedef "ANY" her şeyi sayar. */
public final class QuestModule extends BaseModule implements QuestService, Listener, CommandExecutor {
    static final class Def {
        String id, name = "Görev", desc = "", type = "KILL", target = "ANY";
        int amount = 10, minLevel = 1;
        long gold, xp;
        ItemStack item;
        boolean repeatable;
    }

    final Map<String, Def> defs = new TreeMap<>();
    private YamlStore store;

    public QuestModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "quest"; }

    @Override public void enable() {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "quests.yml"));
        load();
        Services.register(QuestService.class, this);
        listen(this);
        plugin.bind("gorev", this);
        plugin.registerAdmin("gorev", "quest", (p, a) -> new QuestAdminMenu(this).open(p));
    }

    @Override public void disable() {
        cleanup();
        if (store != null) { save(); store.flush(); }
        Services.unregister(QuestService.class);
        plugin.unbind("gorev");
        plugin.unregisterAdmin("gorev");
    }

    @Override public void reload() { store.load(); load(); }

    private void load() {
        defs.clear();
        ConfigurationSection root = store.yaml().getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Def d = new Def();
            d.id = id;
            d.name = s.getString("name", id);
            d.desc = s.getString("desc", "");
            d.type = s.getString("type", "KILL").toUpperCase(Locale.ROOT);
            d.target = s.getString("target", "ANY");
            d.amount = Math.max(1, s.getInt("amount", 10));
            d.gold = s.getLong("gold", 0);
            d.xp = s.getLong("xp", 0);
            d.minLevel = Math.max(1, s.getInt("minLevel", 1));
            d.item = s.getItemStack("item");
            d.repeatable = s.getBoolean("repeatable", false);
            defs.put(id, d);
        }
    }

    void save() {
        YamlConfiguration y = store.yaml();
        y.set("quests", null);
        for (Def d : defs.values()) {
            String b = "quests." + d.id;
            y.set(b + ".name", d.name);
            y.set(b + ".desc", d.desc);
            y.set(b + ".type", d.type);
            y.set(b + ".target", d.target);
            y.set(b + ".amount", d.amount);
            y.set(b + ".gold", d.gold);
            y.set(b + ".xp", d.xp);
            y.set(b + ".minLevel", d.minLevel);
            y.set(b + ".item", d.item);
            y.set(b + ".repeatable", d.repeatable);
        }
        store.dirty();
    }

    // ---------- oyuncu durumu ----------

    ProfileService ps() { return Services.get(ProfileService.class); }

    int progressOf(Player p, String id) { return ps().get(p).section("quests").getInt("active." + id, -1); }

    boolean isActive(Player p, String id) { return progressOf(p, id) >= 0; }

    /** 0 = alınabilir; >0 = tekrar için kalan ms; -1 = bir kez yapıldı. */
    long lockLeft(Player p, Def d) {
        long done = ps().get(p).section("quests").getLong("done." + d.id, 0);
        if (done == 0) return 0;
        if (!d.repeatable) return -1;
        long left = done + cfg().getLong("quest.repeat-hours", 24) * 3600_000L - System.currentTimeMillis();
        return Math.max(0, left);
    }

    int activeCount(Player p) {
        ConfigurationSection a = ps().get(p).section("quests").getConfigurationSection("active");
        return a == null ? 0 : a.getKeys(false).size();
    }

    boolean accept(Player p, Def d) {
        PlayerProfile pr = ps().get(p);
        if (pr.level() < d.minLevel) { Fx.fail(p); Lang.send(p, "gorev.seviye", "<red>Bu görev için seviye <n> gerekir.", "n", String.valueOf(d.minLevel)); return false; }
        if (isActive(p, d.id)) return false;
        if (lockLeft(p, d) != 0) { Fx.fail(p); Lang.send(p, "gorev.kilitli", "<red>Bu görevi şu an alamazsın."); return false; }
        if (activeCount(p) >= cfg().getInt("quest.max-active", 5)) { Fx.fail(p); Lang.send(p, "gorev.dolu", "<red>En fazla <n> aktif görevin olabilir.", "n", String.valueOf(cfg().getInt("quest.max-active", 5))); return false; }
        pr.section("quests").set("active." + d.id, 0);
        pr.save();
        Fx.ok(p);
        Lang.send(p, "gorev.alindi", "<green>Görev alındı: <white><ad>", "ad", d.name);
        return true;
    }

    void abandon(Player p, Def d) {
        PlayerProfile pr = ps().get(p);
        pr.section("quests").set("active." + d.id, null);
        pr.save();
        Lang.send(p, "gorev.birakildi", "<yellow>Görev bırakıldı: <white><ad>", "ad", d.name);
    }

    @Override public List<QuestView> tracked(Player p) {
        List<QuestView> out = new ArrayList<>();
        ConfigurationSection a = ps().get(p).section("quests").getConfigurationSection("active");
        if (a == null) return out;
        for (String id : a.getKeys(false)) {
            Def d = defs.get(id);
            if (d != null) out.add(new QuestView(d.id, d.name, d.desc, a.getInt(id), d.amount, d.gold, d.xp));
        }
        return out;
    }

    private void progress(Player p, String type, String target, int amount) {
        if (defs.isEmpty() || ps() == null) return;
        PlayerProfile pr = ps().get(p);
        ConfigurationSection a = pr.section("quests").getConfigurationSection("active");
        if (a == null) return;
        for (String id : new ArrayList<>(a.getKeys(false))) {
            Def d = defs.get(id);
            if (d == null || !d.type.equals(type)) continue;
            if (!d.target.equalsIgnoreCase("ANY") && !d.target.equalsIgnoreCase(target)) continue;
            int now = Math.min(d.amount, a.getInt(id) + amount);
            a.set(id, now);
            pr.save();
            if (now >= d.amount) complete(p, d);
            else Fx.action(p, Lang.t("gorev.ilerleme", "<yellow><ad> <gray>(<white><n>/<m><gray>)", "ad", d.name, "n", String.valueOf(now), "m", String.valueOf(d.amount)));
        }
    }

    private void complete(Player p, Def d) {
        PlayerProfile pr = ps().get(p);
        pr.section("quests").set("active." + d.id, null);
        pr.section("quests").set("done." + d.id, System.currentTimeMillis());
        pr.save();
        EconomyService eco = Services.get(EconomyService.class);
        if (eco != null && d.gold > 0) eco.give(p.getUniqueId(), d.gold);
        if (d.xp > 0) ps().addXp(p, d.xp, d.name);
        if (!Util.isAir(d.item)) Util.give(p, d.item);
        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(p, d.id));
        Io.log("quest", p.getName() + " görev tamamladı: " + d.id);
        p.showTitle(Title.title(Lang.t("gorev.bitti-baslik", "<gradient:#7bed9f:#2ed573><bold>GÖREV TAMAMLANDI"), Lang.mm("<white>" + d.name),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        Fx.levelUp(p);
        Lang.send(p, "gorev.odul", "<green>Ödül: <gold><altin> Altın <gray>+ <aqua><xp> XP", "altin", Util.num(d.gold), "xp", Util.num(d.xp));
    }

    // ---------- olaylar ----------

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        Player k = m.getKiller();
        if (k == null || m instanceof Player) return;
        var c = m.getPersistentDataContainer();
        if (c.has(Keys.BOSS_ID, PersistentDataType.STRING) || c.has(Keys.NO_REWARD, PersistentDataType.INTEGER)) return;
        progress(k, "KILL", m.getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMine(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        progress(e.getPlayer(), "MINE", e.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onBoss(BossDefeatEvent e) {
        for (UUID id : e.getDamage().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) progress(p, "BOSS", e.getBossId(), 1);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        if (s instanceof Player p) new QuestMenu(this).open(p);
        else Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık.");
        return true;
    }
}
