package tr.sonumut.rpg;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.core.event.ClassChangeEvent;
import tr.sonumut.core.event.RpgLevelUpEvent;
import tr.sonumut.core.event.RpgXpGainEvent;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/** Sınıf, seviye, XP, mana ve temel savaş mekaniği. */
public final class ProfileModule extends BaseModule implements ProfileService, Listener {
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final List<StatSource> sources = new CopyOnWriteArrayList<>();
    /** Yetenek hasarı sırasında normal hasar çarpanları atlanır. */
    public boolean inSkill;

    public ProfileModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "rpg"; }

    @Override public void enable() {
        Services.register(ProfileService.class, this);
        listen(this);
        listen(new SkillListener(plugin, this));
        RpgCommand cmd = new RpgCommand();
        plugin.bind("sinif", cmd);
        plugin.bind("profil", cmd);
        plugin.giveables().put("sinifkarti", extra -> classCard());
        for (Player p : Bukkit.getOnlinePlayers()) load(p);
        every(20L, 20L, this::tick);
    }

    @Override public void disable() {
        cleanup();
        for (PlayerProfile pr : profiles.values()) pr.flush();
        profiles.clear();
        Services.unregister(ProfileService.class);
        plugin.unbind("sinif", "profil");
        plugin.giveables().remove("sinifkarti");
    }

    // ---------- ProfileService ----------

    @Override public PlayerProfile get(Player p) { return get(p.getUniqueId()); }

    @Override public PlayerProfile get(UUID id) {
        PlayerProfile pr = profiles.get(id);
        if (pr != null) return pr;
        pr = new PlayerProfile(id, new YamlStore(plugin, new File(plugin.getDataFolder(), "data/players/" + id + ".yml")));
        profiles.put(id, pr);
        return pr;
    }

    @Override public PlayerProfile peek(UUID id) { return profiles.get(id); }

    @Override public int level(Player p) { return get(p).level(); }

    @Override public ClassType classOf(Player p) { return get(p).cls(); }

    @Override public void registerStatSource(StatSource s) { sources.add(s); }

    @Override public double combatStat(Player p, CombatStat s) {
        PlayerProfile pr = get(p);
        double v = pr.cls() != null ? pr.cls().base(s) : 0;
        v += pr.alloc(s) * s.perPoint;
        for (StatSource src : sources) v += src.bonus(p, s);
        return Math.min(s.cap, Math.max(0, v));
    }

    @Override public long xpNeeded(int level) {
        return cfg().getLong("rpg.xp-base", 100) + (long) level * level * cfg().getLong("rpg.xp-curve", 25);
    }

    @Override public double maxMana(Player p) {
        return cfg().getDouble("rpg.mana-base", 50) + get(p).level() * cfg().getDouble("rpg.mana-per-level", 3);
    }

    @Override public double mana(Player p) {
        PlayerProfile pr = get(p);
        if (pr.mana < 0) pr.mana = maxMana(p);
        return pr.mana;
    }

    @Override public boolean useMana(Player p, double cost) {
        PlayerProfile pr = get(p);
        if (mana(p) < cost) return false;
        pr.mana -= cost;
        return true;
    }

    @Override public void addXp(Player p, long amount, String source) {
        if (amount <= 0 || !p.isOnline()) return;
        PlayerProfile pr = get(p);
        int max = cfg().getInt("rpg.max-level", 100);
        long fin = Math.max(1, Math.round(amount * (1 + combatStat(p, CombatStat.XP_BONUS) / 100.0)));
        Bukkit.getPluginManager().callEvent(new RpgXpGainEvent(p, fin, source));
        if (pr.level() >= max) return;
        int old = pr.level();
        pr.setXp(pr.xp() + fin);
        int pointsPer = cfg().getInt("rpg.points-per-level", 1);
        while (pr.level() < max && pr.xp() >= xpNeeded(pr.level())) {
            pr.setXp(pr.xp() - xpNeeded(pr.level()));
            pr.setLevel(pr.level() + 1);
            pr.addPoints(pointsPer);
        }
        if (pr.level() >= max) pr.setXp(0);
        pr.save();
        Fx.action(p, Lang.t("rpg.xp", "<green>+<miktar> XP <gray>(<kaynak>)", "miktar", Util.num(fin), "kaynak", source));
        if (pr.level() != old) levelUp(p, old, pr.level());
    }

    private void levelUp(Player p, int old, int now) {
        applyAttributes(p);
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a != null) p.setHealth(a.getValue());
        get(p).mana = maxMana(p);
        Bukkit.getPluginManager().callEvent(new RpgLevelUpEvent(p, old, now));
        p.showTitle(Title.title(
                Lang.t("rpg.lvl-baslik", "<gradient:#ffd27f:#ff7e5f><bold>SEVİYE ATLADIN!"),
                Lang.t("rpg.lvl-alt", "<yellow>Seviye <eski> <gray>→ <green><yeni>", "eski", String.valueOf(old), "yeni", String.valueOf(now)),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(600))));
        Fx.levelUp(p);
        Lang.send(p, "rpg.lvl-mesaj", "<green>Yeni seviye: <white><yeni> <gray>(Yetenek puanı: <white><puan><gray>) <yellow>/profil", 
                "yeni", String.valueOf(now), "puan", String.valueOf(get(p).points()));
    }

    @Override public void setClass(Player p, ClassType type) {
        PlayerProfile pr = get(p);
        ClassType old = pr.cls();
        pr.setClass(type);
        pr.save();
        applyAttributes(p);
        Bukkit.getPluginManager().callEvent(new ClassChangeEvent(p, old, type));
        Fx.levelUp(p);
        Lang.send(p, "rpg.sinif-secildi", "<green>Yeni sınıfın: <sinif>", "sinif", type.ad);
        p.sendMessage(Lang.mm(type.gradient()));
    }

    @Override public void applyAttributes(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) return;
        for (AttributeModifier m : new ArrayList<>(a.getModifiers())) {
            if (Keys.HP_MOD.equals(m.getKey())) a.removeModifier(m);
        }
        PlayerProfile pr = get(p);
        double bonus = (pr.level() - 1) * cfg().getDouble("rpg.hp-per-level", 0.5) + (pr.cls() != null ? pr.cls().hpBonus : 0);
        if (bonus != 0) a.addModifier(new AttributeModifier(Keys.HP_MOD, bonus, AttributeModifier.Operation.ADD_NUMBER));
        if (p.getHealth() > a.getValue()) p.setHealth(a.getValue());
    }

    public ItemStack classCard() {
        return ItemBuilder.of(Material.NAME_TAG)
                .name(Lang.t("rpg.kart-ad", "<gradient:#74b9ff:#a29bfe><bold>Sınıf Değiştirme Kartı"))
                .lore(Lang.t("rpg.kart-lore1", "<gray>Sağ tıkla ve yeni sınıfını seç."),
                        Lang.t("rpg.kart-lore2", "<dark_gray>Kart kullanılınca harcanır.")).glow(true).hideAll()
                .str(Keys.CLASS_CARD, "1").build();
    }

    void heal(Player p, double amount) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) return;
        p.setHealth(Math.min(a.getValue(), p.getHealth() + amount));
    }

    // ---------- Olaylar ----------

    private void load(Player p) {
        PlayerProfile pr = get(p);
        pr.setName(p.getName());
        if (pr.mana < 0) pr.mana = maxMana(p);
        applyAttributes(p);
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile pr = get(p);
            double max = maxMana(p);
            pr.mana = Math.min(max, mana(p) + combatStat(p, CombatStat.MANA_YENILEME));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        load(p);
        PlayerProfile pr = get(p);
        if (pr.cls() == null && cfg().getBoolean("rpg.class-menu-on-join", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && get(p).cls() == null) new ClassMenu(this, false).open(p);
            }, 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerProfile pr = profiles.remove(e.getPlayer().getUniqueId());
        if (pr != null) pr.flush();
        Cooldowns.clear(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) applyAttributes(p); }, 2L);
    }

    @EventHandler
    public void onCard(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (ItemBuilder.pdcString(it, Keys.CLASS_CARD) == null) return;
        e.setCancelled(true);
        new ClassMenu(this, true).open(e.getPlayer());
    }

    private static Player shooter(Entity d) {
        if (d instanceof Player p) return p;
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player attacker = shooter(e.getDamager());
        if (!inSkill && attacker != null && e.getEntity() instanceof LivingEntity victim && !attacker.equals(victim)) {
            PlayerProfile pr = get(attacker);
            double dmg = e.getDamage() * (1 + (pr.level() - 1) * cfg().getDouble("rpg.damage-per-level", 0.01));
            if (ThreadLocalRandom.current().nextDouble() * 100 < combatStat(attacker, CombatStat.KRITIK_SANS)) {
                dmg *= 1 + combatStat(attacker, CombatStat.KRITIK_HASAR) / 100.0;
                Fx.burst(victim.getLocation().add(0, 1, 0), Particle.CRIT, 15, 0.4);
                Fx.sound(attacker, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1f);
                Fx.action(attacker, Lang.t("rpg.kritik", "<gold><bold>KRİTİK!"));
            }
            e.setDamage(dmg);
            double steal = combatStat(attacker, CombatStat.CAN_CALMA);
            if (steal > 0) heal(attacker, dmg * steal / 100.0);
        }
        if (e.getEntity() instanceof Player victim) {
            if (ThreadLocalRandom.current().nextDouble() * 100 < combatStat(victim, CombatStat.KACINMA)) {
                e.setCancelled(true);
                Fx.burst(victim.getLocation().add(0, 1, 0), Particle.CLOUD, 10, 0.3);
                Fx.sound(victim, Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.6f);
                Fx.action(victim, Lang.t("rpg.kacindin", "<aqua>KAÇINDIN!"));
                return;
            }
            double refl = combatStat(victim, CombatStat.YANSIMA);
            if (refl > 0) {
                Entity d = e.getDamager();
                LivingEntity src = d instanceof LivingEntity le ? le
                        : (d instanceof Projectile pj && pj.getShooter() instanceof LivingEntity sh ? sh : null);
                if (src != null && !src.equals(victim)) src.damage(e.getDamage() * refl / 100.0);
            }
        }
    }

    // ---------- Komutlar ----------

    private final class RpgCommand implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
            if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
            if (cmd.getName().equals("sinif") && get(p).cls() == null) new ClassMenu(ProfileModule.this, false).open(p);
            else {
                if (cmd.getName().equals("sinif")) Lang.send(p, "rpg.sinif-kalici", "<gray>Sınıfın kalıcıdır. Değiştirmek için <white>Sınıf Değiştirme Kartı <gray>gerekir.");
                new ProfileMenu(ProfileModule.this).open(p);
            }
            return true;
        }
    }
}
