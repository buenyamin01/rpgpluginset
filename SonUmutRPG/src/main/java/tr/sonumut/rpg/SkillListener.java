package tr.sonumut.rpg;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.party.PartyService;

import java.util.Set;
import java.util.UUID;

/** Sınıf yetenekleri: Shift + F (el değiştirme tuşu). Mana harcar, bekleme süresi vardır. */
final class SkillListener implements Listener {
    private final SonUmutPlugin plugin;
    private final ProfileModule mod;

    SkillListener(SonUmutPlugin plugin, ProfileModule mod) {
        this.plugin = plugin;
        this.mod = mod;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        e.setCancelled(true);
        ClassType c = mod.get(p).cls();
        if (c == null) { Lang.send(p, "rpg.sinif-yok", "<red>Önce bir sınıf seçmelisin: <white>/sinif"); return; }
        long left = Cooldowns.left(p.getUniqueId(), "skill");
        if (left > 0) {
            Fx.action(p, Lang.t("rpg.bekleme", "<red>Yetenek bekliyor: <white><sn> sn", "sn", String.format("%.1f", left / 1000.0)));
            return;
        }
        if (!mod.useMana(p, c.manaCost)) {
            Fx.fail(p);
            Fx.action(p, Lang.t("rpg.mana-yok", "<red>Yetersiz mana! <gray>(<mana>)", "mana", String.valueOf(c.manaCost)));
            return;
        }
        Cooldowns.set(p.getUniqueId(), "skill", c.cooldown * 1000L);
        Fx.action(p, Lang.mm("<gradient:" + c.renk1 + ":" + c.renk2 + ">" + c.ikon + " " + c.yetenek + "!"));
        switch (c) {
            case SAVASCI -> slam(p);
            case OKCU -> arrows(p);
            case BUYUCU -> fireball(p);
            case SUIKASTCI -> shadow(p);
            case SIFACI -> healWave(p);
        }
    }

    private static boolean hostile(Entity en, Player caster) {
        return en instanceof Enemy && en instanceof LivingEntity && !en.equals(caster);
    }

    private void hit(Player p, LivingEntity target, double dmg) {
        mod.inSkill = true;
        try { target.damage(dmg, p); } finally { mod.inSkill = false; }
    }

    private void slam(Player p) {
        double dmg = 6 + mod.get(p).level() * 0.4;
        Location l = p.getLocation();
        Fx.sound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
        Fx.burst(l.clone().add(0, 0.2, 0), Particle.EXPLOSION, 1, 0);
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            Fx.burst(l.clone().add(Math.cos(a) * 3.5, 0.2, Math.sin(a) * 3.5), Particle.LARGE_SMOKE, 2, 0.1);
        }
        for (Entity en : p.getNearbyEntities(4, 2, 4)) {
            if (!hostile(en, p)) continue;
            LivingEntity le = (LivingEntity) en;
            hit(p, le, dmg);
            le.setVelocity(new Vector(0, 0.55, 0));
        }
    }

    private void arrows(Player p) {
        double dmg = 2 + mod.get(p).level() * 0.1;
        Fx.sound(p, Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
        for (int i = -2; i <= 2; i++) {
            Vector dir = p.getLocation().getDirection().clone().rotateAroundY(Math.toRadians(i * 8)).multiply(2.5);
            Arrow a = p.launchProjectile(Arrow.class, dir);
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.setDamage(dmg);
            Bukkit.getScheduler().runTaskLater(plugin, a::remove, 100L);
        }
    }

    private void fireball(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        double range = 20;
        RayTraceResult r = p.getWorld().rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.6,
                en -> hostile(en, p));
        double dist = range;
        if (r != null) dist = eye.toVector().distance(r.getHitPosition());
        for (double d = 1; d < dist; d += 0.5) Fx.burst(eye.clone().add(dir.clone().multiply(d)), Particle.FLAME, 2, 0.05);
        Fx.sound(p, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        if (r != null) {
            Location at = r.getHitPosition().toLocation(p.getWorld());
            Fx.burst(at, Particle.EXPLOSION, 1, 0);
            Fx.sound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
            if (r.getHitEntity() instanceof LivingEntity le) {
                hit(p, le, 8 + mod.get(p).level() * 0.6);
                le.setFireTicks(60);
            }
        }
    }

    private void shadow(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.0001) return;
        dir.normalize();
        double max = 6;
        double dist = max;
        for (double yo : new double[]{p.getEyeHeight() - 0.1, 0.3}) {
            Location from = p.getLocation().add(0, yo, 0);
            RayTraceResult r = p.getWorld().rayTraceBlocks(from, dir, max, FluidCollisionMode.NEVER, true);
            if (r != null) dist = Math.min(dist, from.toVector().distance(r.getHitPosition()) - 0.8);
        }
        if (dist < 1) { Fx.fail(p); Cooldowns.set(p.getUniqueId(), "skill", 0); return; }
        Location from = p.getLocation();
        Location dest = from.clone().add(dir.clone().multiply(dist));
        for (double d = 0; d < dist; d += 0.5) Fx.burst(from.clone().add(dir.clone().multiply(d)).add(0, 1, 0), Particle.LARGE_SMOKE, 2, 0.15);
        p.teleport(dest);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
        Fx.sound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
    }

    private void healWave(Player p) {
        double amount = 6 + mod.get(p).level() * 0.3;
        PartyService ps = Services.get(PartyService.class);
        Set<UUID> ids = ps != null ? ps.sharers(p, p.getLocation()) : Set.of(p.getUniqueId());
        Fx.sound(p, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.6f);
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            Fx.burst(p.getLocation().add(Math.cos(a) * 4, 0.3, Math.sin(a) * 4), Particle.HAPPY_VILLAGER, 1, 0.1);
        }
        for (UUID id : ids) {
            Player t = Bukkit.getPlayer(id);
            if (t == null || !t.getWorld().equals(p.getWorld()) || t.getLocation().distanceSquared(p.getLocation()) > 64) continue;
            mod.heal(t, amount);
            Fx.burst(t.getLocation().add(0, 1.5, 0), Particle.HEART, 4, 0.3);
        }
        mod.heal(p, 0);
    }
}
