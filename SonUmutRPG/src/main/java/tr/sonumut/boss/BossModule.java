package tr.sonumut.boss;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.core.event.BossDefeatEvent;
import tr.sonumut.mob.MobLevelService;
import tr.sonumut.rpg.CombatStat;
import tr.sonumut.rpg.ProfileService;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Bosslar: bosses.yml. Can çubuğu, fazlar (%66/%33/%20), yer sarsıntısı, hasara göre ödül, yeniden doğma. */
public final class BossModule extends BaseModule implements BossService, Listener {
    static final class Drop {
        ItemStack item;
        double chance = 10;
    }

    static final class Def {
        String id, name = "<red>Boss", spawn;
        EntityType type = EntityType.ZOMBIE;
        double health = 300, damage = 8;
        int level = 10, respawn = 30;
        long xp = 300, gold = 200, lastKill;
        boolean alive;
        final List<Drop> drops = new ArrayList<>();
    }

    private static final class Inst {
        final Def def;
        final LivingEntity ent;
        final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        final Map<UUID, Double> dmg = new LinkedHashMap<>();
        final Set<UUID> viewers = new HashSet<>();
        boolean p66, p33, enraged;
        int ticks;
        Inst(Def def, LivingEntity ent) { this.def = def; this.ent = ent; }
    }

    final Map<String, Def> defs = new TreeMap<>();
    private final Map<UUID, Inst> insts = new HashMap<>();
    private YamlStore store;

    public BossModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "boss"; }

    @Override public void enable() {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "bosses.yml"));
        load();
        Services.register(BossService.class, this);
        listen(this);
        plugin.registerAdmin("boss", "boss", this::admin);
        for (World w : Bukkit.getWorlds()) for (LivingEntity le : w.getLivingEntities()) tryRegister(le);
        every(10L, 10L, this::tick);
        every(100L, 100L, this::respawnCheck);
    }

    @Override public void disable() {
        cleanup();
        for (Inst in : insts.values()) hideAll(in);
        insts.clear();
        if (store != null) { save(); store.flush(); }
        Services.unregister(BossService.class);
        plugin.unregisterAdmin("boss");
    }

    @Override public void reload() { store.load(); load(); }

    @Override public String bossId(Entity e) { return e.getPersistentDataContainer().get(Keys.BOSS_ID, PersistentDataType.STRING); }

    // ---------- veri ----------

    private void load() {
        defs.clear();
        ConfigurationSection root = store.yaml().getConfigurationSection("bosses");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Def d = new Def();
            d.id = id;
            d.name = s.getString("name", d.name);
            try { d.type = EntityType.valueOf(s.getString("type", "ZOMBIE").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { }
            d.health = Math.max(1, s.getDouble("health", 300));
            d.damage = Math.max(0, s.getDouble("damage", 8));
            d.level = Math.max(1, s.getInt("level", 10));
            d.xp = s.getLong("xp", 300);
            d.gold = s.getLong("gold", 200);
            d.respawn = Math.max(0, s.getInt("respawn", 30));
            d.spawn = s.getString("spawn");
            d.alive = s.getBoolean("alive", false);
            d.lastKill = s.getLong("lastKill", 0);
            ConfigurationSection ds = s.getConfigurationSection("drops");
            if (ds != null) {
                for (String k : ds.getKeys(false)) {
                    ConfigurationSection x = ds.getConfigurationSection(k);
                    if (x == null) continue;
                    Drop dr = new Drop();
                    dr.item = x.getItemStack("item");
                    dr.chance = x.getDouble("chance", 10);
                    if (!Util.isAir(dr.item)) d.drops.add(dr);
                }
            }
            defs.put(id, d);
        }
    }

    void save() {
        YamlConfiguration y = store.yaml();
        y.set("bosses", null);
        for (Def d : defs.values()) {
            String b = "bosses." + d.id;
            y.set(b + ".name", d.name);
            y.set(b + ".type", d.type.name());
            y.set(b + ".health", d.health);
            y.set(b + ".damage", d.damage);
            y.set(b + ".level", d.level);
            y.set(b + ".xp", d.xp);
            y.set(b + ".gold", d.gold);
            y.set(b + ".respawn", d.respawn);
            y.set(b + ".spawn", d.spawn);
            y.set(b + ".alive", d.alive);
            y.set(b + ".lastKill", d.lastKill);
            int i = 0;
            for (Drop dr : d.drops) {
                y.set(b + ".drops." + i + ".item", dr.item);
                y.set(b + ".drops." + i + ".chance", dr.chance);
                i++;
            }
        }
        store.dirty();
    }

    // ---------- yönetici komutları ----------

    private void admin(Player p, String[] a) {
        if (a.length == 0) { new BossAdminMenu(this).open(p); return; }
        Def d = a.length > 1 ? defs.get(a[1].toLowerCase(Locale.ROOT)) : null;
        if (d == null) { Lang.send(p, "boss.yok", "<red>Boss bulunamadı. <gray>Kullanım: <white>/sonumut boss spawn|drop|respawn <id> ..."); return; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> spawnNow(p, d);
            case "drop" -> {
                ItemStack h = p.getInventory().getItemInMainHand();
                if (Util.isAir(h)) { Lang.send(p, "boss.el-bos", "<red>Ana elinde eşya yok."); return; }
                Drop dr = new Drop();
                dr.item = h.clone();
                dr.chance = a.length > 2 ? Util.clamp(Util.parseDouble(a[2], 10), 0.001, 100) : 10;
                d.drops.add(dr);
                save();
                Lang.send(p, "boss.drop-eklendi", "<green>Düşme eklendi (<sans>%).", "sans", String.valueOf(dr.chance));
            }
            case "respawn" -> {
                d.respawn = a.length > 2 ? Math.max(0, Util.parseInt(a[2], d.respawn)) : d.respawn;
                save();
                Lang.send(p, "boss.respawn-ayar", "<green>Yeniden doğma: <white><dk> dk", "dk", String.valueOf(d.respawn));
            }
            default -> Lang.send(p, "boss.kullanim", "<gray>Kullanım: <white>/sonumut boss spawn|drop|respawn <id>");
        }
    }

    void spawnNow(Player p, Def d) {
        Location l = Util.parseLoc(d.spawn);
        if (l == null) l = p.getLocation();
        else l.add(0.5, 0, 0.5);
        if (spawn(d, l) == null) Lang.send(p, "boss.spawn-hata", "<red>Boss doğurulamadı."); else Fx.ok(p);
    }

    void resetAlive(Def d) {
        for (Inst in : new ArrayList<>(insts.values())) {
            if (in.def == d) { hideAll(in); insts.remove(in.ent.getUniqueId()); in.ent.remove(); }
        }
        d.alive = false;
        save();
    }

    void delete(Def d) {
        resetAlive(d);
        defs.remove(d.id);
        save();
    }

    // ---------- doğurma ----------

    LivingEntity spawn(Def d, Location loc) {
        World w = loc.getWorld();
        if (w == null || !d.type.isAlive() || !d.type.isSpawnable()) return null;
        Entity en = w.spawnEntity(loc, d.type);
        if (!(en instanceof LivingEntity le)) { en.remove(); return null; }
        le.getPersistentDataContainer().set(Keys.BOSS_ID, PersistentDataType.STRING, d.id);
        le.getPersistentDataContainer().set(Keys.MOB_LEVEL, PersistentDataType.INTEGER, d.level);
        AttributeInstance h = le.getAttribute(Attribute.MAX_HEALTH);
        if (h != null) { h.setBaseValue(Math.min(1024, d.health)); le.setHealth(h.getValue()); }
        AttributeInstance dm = le.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dm != null) dm.setBaseValue(d.damage);
        le.customName(Lang.mm(d.name));
        le.setCustomNameVisible(true);
        le.setRemoveWhenFarAway(false);
        le.setPersistent(true);
        d.alive = true;
        save();
        register(d, le);
        Fx.sound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        Fx.burst(loc.clone().add(0, 1, 0), Particle.EXPLOSION, 3, 0.5);
        Bukkit.broadcast(Lang.t("boss.dogdu", "<dark_red>☠ <red><bold><ad></bold> <gray>ortaya çıktı! <dark_gray>(<dunya> <x>, <y>, <z>)",
                "ad", Lang.plain(Lang.mm(d.name)), "dunya", w.getName(), "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ())));
        return le;
    }

    private Inst register(Def d, LivingEntity le) {
        Inst in = insts.get(le.getUniqueId());
        if (in == null) { in = new Inst(d, le); insts.put(le.getUniqueId(), in); }
        return in;
    }

    private void tryRegister(Entity e) {
        if (!(e instanceof LivingEntity le)) return;
        String id = bossId(le);
        if (id == null || insts.containsKey(le.getUniqueId())) return;
        Def d = defs.get(id);
        if (d == null) { le.remove(); return; }
        register(d, le);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) { for (Entity en : e.getEntities()) tryRegister(en); }

    // ---------- zamanlayıcı ----------

    private void hideAll(Inst in) {
        for (UUID id : in.viewers) { Player p = Bukkit.getPlayer(id); if (p != null) p.hideBossBar(in.bar); }
        in.viewers.clear();
    }

    private void tick() {
        double barR = cfg().getDouble("boss.bar-radius", 40), slamR = cfg().getDouble("boss.slam-radius", 5);
        int slamT = cfg().getInt("boss.slam-ticks", 160);
        for (Inst in : new ArrayList<>(insts.values())) {
            LivingEntity le = in.ent;
            if (le.isDead()) { hideAll(in); insts.remove(le.getUniqueId()); continue; }
            if (!le.isValid()) { hideAll(in); continue; }
            AttributeInstance h = le.getAttribute(Attribute.MAX_HEALTH);
            double max = h != null ? h.getValue() : in.def.health, hp = le.getHealth(), frac = Util.clamp(hp / max, 0, 1);
            in.bar.progress((float) frac);
            in.bar.name(Lang.mm(in.def.name + " <gray>" + (int) Math.ceil(hp) + "/" + (int) max));
            for (Player p : le.getWorld().getPlayers()) {
                boolean near = p.getLocation().distanceSquared(le.getLocation()) <= barR * barR;
                if (near && in.viewers.add(p.getUniqueId())) p.showBossBar(in.bar);
                else if (!near && in.viewers.remove(p.getUniqueId())) p.hideBossBar(in.bar);
            }
            in.viewers.removeIf(id -> Bukkit.getPlayer(id) == null);
            in.ticks += 10;
            if (!in.p66 && frac <= 0.66) { in.p66 = true; summon(in, 3, "<red>Uşaklarını çağırdı!"); }
            if (!in.p33 && frac <= 0.33) { in.p33 = true; summon(in, 4, "<dark_red>Orduyu çağırdı!"); }
            if (!in.enraged && frac <= 0.20) {
                in.enraged = true;
                le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 600, 1));
                le.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 600, 1));
                Fx.burst(le.getLocation().add(0, 1.5, 0), Particle.ANGRY_VILLAGER, 15, 0.8);
                announce(in, "<dark_red><bold>ÖFKELENDİ!");
            }
            if (in.ticks % slamT == 0) slam(in, slamR);
        }
    }

    private void announce(Inst in, String mm) {
        for (UUID id : in.viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(Lang.mm(in.def.name + " " + mm));
        }
    }

    private void slam(Inst in, double r) {
        LivingEntity le = in.ent;
        boolean any = false;
        for (Player p : le.getWorld().getPlayers()) if (p.getLocation().distanceSquared(le.getLocation()) <= (r + 3) * (r + 3)) { any = true; break; }
        if (!any) return;
        Location l = le.getLocation();
        Fx.sound(l, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        for (int i = 0; i < 36; i++) {
            double a = Math.toRadians(i * 10);
            Fx.burst(l.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r), Particle.LARGE_SMOKE, 2, 0.1);
        }
        for (Player p : le.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(l) > r * r) continue;
            p.damage(in.def.damage * 1.5, le);
            p.setVelocity(new Vector(0, 0.6, 0));
        }
    }

    private void summon(Inst in, int count, String msg) {
        announce(in, msg);
        LivingEntity boss = in.ent;
        MobLevelService ml = Services.get(MobLevelService.class);
        for (int i = 0; i < count; i++) {
            Location l = boss.getLocation().add(ThreadLocalRandom.current().nextDouble(-3, 3), 0, ThreadLocalRandom.current().nextDouble(-3, 3));
            Entity en = boss.getWorld().spawnEntity(l, EntityType.ZOMBIE);
            if (!(en instanceof LivingEntity m)) { en.remove(); continue; }
            m.getPersistentDataContainer().set(Keys.NO_REWARD, PersistentDataType.INTEGER, 1);
            m.getPersistentDataContainer().set(Keys.MINION, PersistentDataType.STRING, in.def.id);
            if (ml != null) ml.apply(m, Math.max(1, in.def.level - 5));
            Fx.burst(l.clone().add(0, 1, 0), Particle.LARGE_SMOKE, 8, 0.3);
        }
        Fx.sound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
    }

    private void respawnCheck() {
        long now = System.currentTimeMillis();
        for (Def d : defs.values()) {
            if (d.alive || d.spawn == null) continue;
            if (d.lastKill > 0 && (d.respawn <= 0 || now < d.lastKill + d.respawn * 60_000L)) continue;
            Location l = Util.parseLoc(d.spawn);
            if (l == null || l.getWorld() == null || !l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
            spawn(d, l.add(0.5, 0, 0.5));
        }
    }

    // ---------- savaş ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Inst in = insts.get(e.getEntity().getUniqueId());
        if (in == null) return;
        Entity d = e.getDamager();
        Player p = d instanceof Player pl ? pl : (d instanceof Projectile pr && pr.getShooter() instanceof Player pl2 ? pl2 : null);
        if (p != null) in.dmg.merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        Inst in = insts.remove(e.getEntity().getUniqueId());
        if (in == null) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        hideAll(in);
        Def d = in.def;
        d.alive = false;
        d.lastKill = System.currentTimeMillis();
        save();

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(in.dmg.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<UUID, Double> ordered = new LinkedHashMap<>();
        double total = 0;
        for (Map.Entry<UUID, Double> en : sorted) { ordered.put(en.getKey(), en.getValue()); total += en.getValue(); }
        Bukkit.getPluginManager().callEvent(new BossDefeatEvent(d.id, ordered));

        ProfileService ps = Services.get(ProfileService.class);
        EconomyService eco = Services.get(EconomyService.class);
        StringBuilder top = new StringBuilder();
        int rank = 0;
        for (Map.Entry<UUID, Double> en : ordered.entrySet()) {
            rank++;
            Player p = Bukkit.getPlayer(en.getKey());
            if (rank <= 3) top.append(rank).append(". ").append(PartyName.of(en.getKey())).append(" (").append((int) (double) en.getValue()).append("), ");
            if (p == null) continue;
            double share = total <= 0 ? 0 : en.getValue() / total;
            double mult = Math.max(0.1, share) + (rank == 1 ? 0.15 : 0);
            if (ps != null) ps.addXp(p, Math.round(d.xp * mult), Lang.plain(Lang.mm(d.name)));
            if (eco != null) eco.give(p.getUniqueId(), Math.round(d.gold * mult));
            double bonus = ps != null ? ps.combatStat(p, CombatStat.GANIMET_SANSI) : 0;
            for (Drop dr : d.drops) {
                if (ThreadLocalRandom.current().nextDouble() * 100 < dr.chance * (1 + bonus / 100.0)) Util.give(p, dr.item);
            }
            p.showTitle(Title.title(Lang.t("boss.yenildi-baslik", "<gold><bold>BOSS YENİLDİ"), Lang.mm(d.name),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(600))));
            Fx.levelUp(p);
        }
        Io.log("boss", d.id + " yenildi. Katılımcı: " + ordered.size());
        Bukkit.broadcast(Lang.t("boss.yenildi", "<gold>☠ <yellow><ad> <gray>yenildi! <dark_gray>En çok hasar: <white><liste>",
                "ad", Lang.plain(Lang.mm(d.name)), "liste", top.length() > 2 ? top.substring(0, top.length() - 2) : "-"));
    }

    /** Çevrimdışı olabilecek oyuncu adı. */
    private static final class PartyName {
        static String of(UUID id) { String n = Bukkit.getOfflinePlayer(id).getName(); return n == null ? "?" : n; }
    }
}
