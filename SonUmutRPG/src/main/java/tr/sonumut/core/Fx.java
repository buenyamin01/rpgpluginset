package tr.sonumut.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Ses + parçacık yardımcıları. */
public final class Fx {
    private Fx() {}

    public static void sound(Player p, Sound s, float vol, float pitch) { p.playSound(p.getLocation(), s, vol, pitch); }

    public static void sound(Location l, Sound s, float vol, float pitch) {
        if (l.getWorld() != null) l.getWorld().playSound(l, s, vol, pitch);
    }

    public static void burst(Location l, Particle type, int count, double spread) {
        if (l.getWorld() != null) l.getWorld().spawnParticle(type, l, count, spread, spread, spread, 0.02);
    }

    public static void dust(Location l, Color c, float size, int count, double spread) {
        if (l.getWorld() != null)
            l.getWorld().spawnParticle(Particle.DUST, l, count, spread, spread, spread, 0, new Particle.DustOptions(c, size));
    }

    public static void ok(Player p) { sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f); }

    public static void fail(Player p) { sound(p, Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f); }

    public static void click(Player p) { sound(p, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f); }

    public static void levelUp(Player p) {
        sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        burst(p.getLocation().add(0, 1, 0), Particle.FIREWORK, 30, 0.6);
    }

    /** Actionbar: HUD varsa onun bastırma mantığı ile. */
    public static void action(Player p, Component c) {
        HudService h = Services.get(HudService.class);
        if (h != null) h.flash(p, c); else p.sendActionBar(c);
    }
}
