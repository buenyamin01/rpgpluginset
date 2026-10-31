package tr.sonumut.crate;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import tr.sonumut.core.*;
import tr.sonumut.gui.Menu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Kayan şerit animasyonu (yalnızca görsel; ödül zaten verildi). */
final class CrateMenu extends Menu {
    private static final int LEN = 40;
    private final List<ItemStack> strip = new ArrayList<>();
    private int idx;
    private int nextAt;

    CrateMenu(CrateModule mod, CrateModule.Crate c, CrateModule.Reward win) {
        super(Lang.mm(c.name), 3);
        for (int i = 0; i < LEN; i++) strip.add(CrateModule.rewardIcon(c.rewards.get(ThreadLocalRandom.current().nextInt(c.rewards.size()))));
        strip.set(LEN - 5, CrateModule.rewardIcon(win));
        locked = true;
    }

    @Override protected void render() {
        fill(Material.BLACK_STAINED_GLASS_PANE);
        set(4, ItemBuilder.of(Material.HOPPER).name(" ").build());
        set(22, ItemBuilder.of(Material.END_ROD).name(" ").build());
        BukkitRunnable[] h = new BukkitRunnable[1];
        h[0] = animate(1L, f -> {
            if (f < nextAt) return;
            if (idx > LEN - 9) {
                h[0].cancel();
                finish();
                return;
            }
            for (int s = 0; s < 9; s++) inv.setItem(9 + s, strip.get(idx + s));
            Fx.sound(viewer, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.0f + idx * 0.02f);
            idx++;
            nextAt = f + 1 + idx / 6;
        });
    }

    private void finish() {
        locked = false;
        Fx.sound(viewer, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        Fx.burst(viewer.getLocation().add(0, 1.3, 0), Particle.FIREWORK, 30, 0.5);
        Player p = viewer;
        // Kapatma düğmesi yok; oyuncu ESC ile kapatır.
        Lang.send(p, "kutu.kazandin", "<green>Ödülün envanterine eklendi!");
    }
}
