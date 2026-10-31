package tr.sonumut.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.core.event.MobKillEvent;
import tr.sonumut.party.PartyService;
import tr.sonumut.rpg.ProfileService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Mob seviyesi: doğduğu yerin çevresindeki oyuncuların seviye ortalamasına göre. Tüm dünyalarda geçerli. */
public final class MobLevelModule extends BaseModule implements MobLevelService, Listener {

    public MobLevelModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "moblevel"; }

    @Override public void enable() {
        Services.register(MobLevelService.class, this);
        listen(this);
    }

    @Override public void disable() {
        cleanup();
        Services.unregister(MobLevelService.class);
    }

    @Override public int levelOf(LivingEntity mob) {
        Integer v = mob.getPersistentDataContainer().get(Keys.MOB_LEVEL, PersistentDataType.INTEGER);
        return v == null ? 1 : v;
    }

    @Override public void apply(LivingEntity m, int level) {
        level = Math.max(1, Math.min(level, cfg().getInt("moblevel.max-level", 100)));
        m.getPersistentDataContainer().set(Keys.MOB_LEVEL, PersistentDataType.INTEGER, level);
        double hpMul = 1 + (level - 1) * cfg().getDouble("moblevel.health-per-level", 0.10);
        double dmgMul = 1 + (level - 1) * cfg().getDouble("moblevel.damage-per-level", 0.04);
        AttributeInstance h = m.getAttribute(Attribute.MAX_HEALTH);
        if (h != null) {
            h.setBaseValue(Math.min(1024, h.getBaseValue() * hpMul));
            m.setHealth(Math.min(h.getValue(), h.getBaseValue()));
        }
        AttributeInstance d = m.getAttribute(Attribute.ATTACK_DAMAGE);
        if (d != null) d.setBaseValue(d.getBaseValue() * dmgMul);
        updateName(m);
    }

    private void updateName(LivingEntity m) {
        if (m.getPersistentDataContainer().has(Keys.BOSS_ID, PersistentDataType.STRING)) return;
        var cn = m.customName();
        if (cn != null && !Lang.plain(cn).startsWith("[Sv.")) return;
        int lvl = levelOf(m);
        String color = lvl < 10 ? "<green>" : lvl < 30 ? "<yellow>" : lvl < 60 ? "<gold>" : "<red>";
        StringBuilder sb = new StringBuilder(color).append("[Sv.").append(lvl).append("] <white>").append(Util.pretty(m.getType().name()));
        AttributeInstance h = m.getAttribute(Attribute.MAX_HEALTH);
        if (cfg().getBoolean("moblevel.show-health", true) && h != null) {
            sb.append(" <red>❤ ").append((int) Math.ceil(m.getHealth())).append("<gray>/<red>").append((int) Math.ceil(h.getValue()));
        }
        m.customName(Lang.mm(sb.toString()));
        m.setCustomNameVisible(true);
    }

    private int computeLevel(Location loc) {
        ProfileService ps = Services.get(ProfileService.class);
        if (ps == null || loc.getWorld() == null) return 1;
        double r = cfg().getDouble("moblevel.radius", 80);
        double sum = 0;
        int n = 0;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= r * r) { sum += ps.level(p); n++; }
        }
        if (n == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) { sum += ps.level(p); n++; }
        }
        int base = n == 0 ? 1 : (int) Math.round(sum / n);
        int spread = cfg().getInt("moblevel.spread", 2);
        int lvl = base + (spread > 0 ? ThreadLocalRandom.current().nextInt(-spread, spread + 1) : 0);
        return Math.max(1, Math.min(lvl, cfg().getInt("moblevel.max-level", 100)));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        LivingEntity m = e.getEntity();
        if (!(m instanceof Enemy) || m instanceof Player) return;
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        PersistentDataContainer c = m.getPersistentDataContainer();
        if (c.has(Keys.BOSS_ID, PersistentDataType.STRING) || c.has(Keys.MOB_LEVEL, PersistentDataType.INTEGER)) return;
        apply(m, computeLevel(m.getLocation()));
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) c.set(Keys.NO_REWARD, PersistentDataType.INTEGER, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!cfg().getBoolean("moblevel.show-health", true)) return;
        if (!(e.getEntity() instanceof LivingEntity m) || m instanceof Player) return;
        if (!m.getPersistentDataContainer().has(Keys.MOB_LEVEL, PersistentDataType.INTEGER)) return;
        Bukkit.getScheduler().runTask(plugin, () -> { if (m.isValid()) updateName(m); });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (m instanceof Player) return;
        PersistentDataContainer c = m.getPersistentDataContainer();
        if (c.has(Keys.BOSS_ID, PersistentDataType.STRING) || c.has(Keys.NO_REWARD, PersistentDataType.INTEGER)) return;
        if (!c.has(Keys.MOB_LEVEL, PersistentDataType.INTEGER)) return;
        Player killer = m.getKiller();
        ProfileService ps = Services.get(ProfileService.class);
        if (killer == null || ps == null) return;
        int lvl = levelOf(m);
        ps.get(killer).addKill();
        Bukkit.getPluginManager().callEvent(new MobKillEvent(killer, m, lvl));

        double diff = lvl - ps.level(killer);
        double mult = Util.clamp(1 + diff * 0.1, 0.1, 2.0);
        long xp = Math.max(1, Math.round((cfg().getLong("moblevel.xp-base", 6) + lvl * cfg().getLong("moblevel.xp-per-level", 2)) * mult));
        PartyService party = Services.get(PartyService.class);
        Set<UUID> ids = party != null ? party.sharers(killer, m.getLocation()) : Set.of(killer.getUniqueId());
        double share = ids.size() > 1 ? (1 + 0.1 * (ids.size() - 1)) / ids.size() : 1.0;
        String src = Util.pretty(m.getType().name());
        for (UUID id : ids) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) ps.addXp(pl, Math.max(1, Math.round(xp * share)), src);
        }
        EconomyService eco = Services.get(EconomyService.class);
        if (eco != null) {
            long gold = Math.max(1, Math.round(lvl * cfg().getDouble("moblevel.gold-per-level", 0.5) * (0.5 + ThreadLocalRandom.current().nextDouble())));
            eco.give(killer.getUniqueId(), gold);
            Fx.action(killer, Lang.t("mob.odul", "<green>+<xp> XP  <gold>+<altin> Altın", "xp", Util.num(Math.max(1, Math.round(xp * share))), "altin", Util.num(gold)));
        }
    }
}
