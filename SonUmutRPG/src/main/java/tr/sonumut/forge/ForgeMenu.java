package tr.sonumut.forge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import tr.sonumut.core.*;
import tr.sonumut.core.event.ItemUpgradeEvent;
import tr.sonumut.gui.Menu;

import java.util.concurrent.ThreadLocalRandom;

/** Eşya geliştirme menüsü: ortadaki slota eşya konur, düğmeye basılır. */
final class ForgeMenu extends Menu {
    private static final int ITEM = 13, BUTTON = 22;
    private static final Material[] FRAMES = {Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE};
    private final ForgeModule mod;

    ForgeMenu(ForgeModule mod) {
        super(Lang.t("gui.demirci-baslik", "<gradient:#f6d365:#fda085><bold>Demirci"), 3);
        this.mod = mod;
        freeSlot(ITEM);
    }

    @Override protected void render() {
        border(Material.GRAY_STAINED_GLASS_PANE);
        fill(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack it = inv.getItem(ITEM);
        ItemBuilder b = ItemBuilder.of(Material.ANVIL).name(Lang.t("gui.demirci-dugme", "<gold><bold>GELİŞTİR"));
        if (!ForgeModule.eligible(it)) {
            b.lore("<gray>Ortaya bir silah ya da zırh koy.");
        } else {
            int cur = ForgeModule.plus(it);
            if (cur >= mod.maxLevel()) b.lore("<green>Bu eşya en üst seviyede.");
            else {
                int t = cur + 1;
                b.lore("<gray>Şu an: <white>+" + cur + " <gray>→ <green>+" + t,
                        "<gray>Başarı şansı: <white>" + mod.chance(t) + "%",
                        "<gray>Ücret: <gold>" + Util.num(mod.cost(t)) + " Altın");
                if (t > mod.failDropFrom()) b.lore("<red>Başarısızlıkta seviye 1 düşer!");
                b.lore(" ", "<yellow>Tıkla ve dene");
            }
        }
        set(BUTTON, b.build(), (p, e) -> attempt(p));
    }

    @Override protected void onFreeChange(Player p) { refresh(); }

    private void attempt(Player p) {
        ItemStack it = inv.getItem(ITEM);
        if (!ForgeModule.eligible(it)) { Fx.fail(p); Lang.send(p, "forge.gecersiz", "<red>Yalnızca silah ve zırh geliştirilebilir."); return; }
        int cur = ForgeModule.plus(it);
        if (cur >= mod.maxLevel()) { Fx.fail(p); Lang.send(p, "forge.en-ust", "<yellow>Bu eşya zaten en üst seviyede."); return; }
        EconomyService eco = Services.get(EconomyService.class);
        int target = cur + 1;
        long cost = mod.cost(target);
        if (eco == null || !eco.take(p.getUniqueId(), cost)) {
            Fx.fail(p);
            Lang.send(p, "forge.altin-yok", "<red>Yeterli Altının yok. Gereken: <gold><miktar>", "miktar", Util.num(cost));
            return;
        }
        // Sonuç animasyondan ÖNCE sunucuda belirlenir.
        boolean ok = ThreadLocalRandom.current().nextDouble() * 100 < mod.chance(target);
        int newPlus = ok ? target : (target > mod.failDropFrom() ? Math.max(0, cur - 1) : cur);
        ItemStack result = it.clone();
        ForgeModule.setPlus(result, newPlus);
        inv.setItem(ITEM, result);
        Bukkit.getPluginManager().callEvent(new ItemUpgradeEvent(p, result, newPlus, ok));
        Io.log("forge", p.getName() + " " + result.getType() + " +" + cur + " -> +" + newPlus + (ok ? " BAŞARILI" : " BAŞARISIZ") + " ücret=" + cost);

        locked = true;
        BukkitRunnable[] h = new BukkitRunnable[1];
        h[0] = animate(3L, f -> {
            if (f < 10) {
                Material m = FRAMES[f % FRAMES.length];
                for (int i = 0; i < 27; i++) if (i != ITEM && i != BUTTON) inv.setItem(i, pane(m));
                Fx.sound(p, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1f + f * 0.08f);
            } else {
                h[0].cancel();
                locked = false;
                if (ok) {
                    Fx.sound(p, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                    Fx.burst(p.getLocation().add(0, 1.2, 0), Particle.FIREWORK, 25, 0.5);
                    Lang.send(p, "forge.basari", "<green>Geliştirme başarılı! <gold>+<n>", "n", String.valueOf(newPlus));
                } else {
                    Fx.sound(p, Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
                    Fx.burst(p.getLocation().add(0, 1.2, 0), Particle.SMOKE, 20, 0.4);
                    Lang.send(p, "forge.basarisiz", "<red>Geliştirme başarısız. <gray>Eşya şu an: <white>+<n>", "n", String.valueOf(newPlus));
                }
                refresh();
            }
        });
    }
}
