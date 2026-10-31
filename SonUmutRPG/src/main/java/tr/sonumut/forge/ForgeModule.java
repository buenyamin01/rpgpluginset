package tr.sonumut.forge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;

import java.util.ArrayList;
import java.util.List;

/** Eşya geliştirme (+1 ... +10). Silah hasarı ve zırh savunması bonusu hasar olayında uygulanır. */
public final class ForgeModule extends BaseModule implements Listener {

    public ForgeModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "forge"; }

    @Override public void enable() {
        listen(this);
        plugin.bind("demirci", new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
                if (s instanceof Player p) new ForgeMenu(ForgeModule.this).open(p);
                else Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık.");
                return true;
            }
        });
    }

    @Override public void disable() {
        cleanup();
        plugin.unbind("demirci");
    }

    // ---------- yardımcılar ----------

    public static int plus(ItemStack it) { return ItemBuilder.pdcInt(it, Keys.UPGRADE, 0); }

    public static boolean isWeapon(ItemStack it) {
        if (Util.isAir(it)) return false;
        String n = it.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("MACE") || n.equals("TRIDENT") || n.equals("BOW") || n.equals("CROSSBOW");
    }

    public static boolean isArmor(ItemStack it) {
        if (Util.isAir(it)) return false;
        String n = it.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    public static boolean eligible(ItemStack it) { return isWeapon(it) || isArmor(it); }

    /** Eşyanın geliştirme seviyesini ayarlar (PDC + "⚒ +N" lore satırı). */
    public static void setPlus(ItemStack it, int n) {
        ItemMeta meta = it.getItemMeta();
        if (n > 0) meta.getPersistentDataContainer().set(Keys.UPGRADE, PersistentDataType.INTEGER, n);
        else meta.getPersistentDataContainer().remove(Keys.UPGRADE);
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.removeIf(c -> Lang.plain(c).startsWith("⚒"));
        if (n > 0) lore.add(0, Lang.mm("<gold>⚒ +" + n + " <gray>Geliştirme").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(lore.isEmpty() ? null : lore);
        if (n >= 7) meta.setEnchantmentGlintOverride(true);
        else if (meta.hasEnchantmentGlintOverride() && Boolean.TRUE.equals(meta.getEnchantmentGlintOverride())) meta.setEnchantmentGlintOverride(null);
        it.setItemMeta(meta);
    }

    double chance(int target) {
        List<Double> l = cfg().getDoubleList("forge.chances");
        if (target - 1 < l.size() && target >= 1) return l.get(target - 1);
        return 5;
    }

    long cost(int target) { return cfg().getLong("forge.cost-base", 100) * (long) target * target; }

    int maxLevel() { return cfg().getInt("forge.max-level", 10); }

    int failDropFrom() { return cfg().getInt("forge.fail-drop-from", 6); }

    // ---------- hasar bonusu ----------

    private static Player shooter(Entity d) {
        if (d instanceof Player p) return p;
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        double dmg = e.getDamage();
        Player attacker = shooter(e.getDamager());
        if (attacker != null) {
            ItemStack hand = attacker.getInventory().getItemInMainHand();
            int n = isWeapon(hand) ? plus(hand) : 0;
            if (n > 0) dmg *= 1 + n * cfg().getDouble("forge.weapon-bonus", 0.04);
        }
        if (e.getEntity() instanceof Player victim) {
            int sum = 0;
            for (ItemStack a : victim.getInventory().getArmorContents()) if (isArmor(a)) sum += plus(a);
            if (sum > 0) dmg *= 1 - Math.min(0.6, sum * cfg().getDouble("forge.armor-bonus", 0.015));
        }
        e.setDamage(dmg);
    }
}
