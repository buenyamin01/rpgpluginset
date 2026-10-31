package tr.sonumut.megaphone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;

import java.time.Duration;

/** Megafon eşyası: sağ tık, mesajı sohbetten yaz, tüm sunucuya başlık + sohbet + ses olarak yayılır. Eşya harcanır. */
public final class MegaphoneModule extends BaseModule implements Listener, CommandExecutor {

    public MegaphoneModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "megaphone"; }

    @Override public void enable() {
        listen(this);
        plugin.bind("megafon", this);
        plugin.giveables().put("megafon", extra -> item());
    }

    @Override public void disable() {
        cleanup();
        plugin.unbind("megafon");
        plugin.giveables().remove("megafon");
    }

    public ItemStack item() {
        return ItemBuilder.of(Material.GOAT_HORN).name(Lang.t("megafon.esya", "<gradient:#ff5555:#ffaa00><bold>Megafon"))
                .lore(Lang.t("megafon.lore1", "<gray>Sağ tıkla, mesajını yaz,"), Lang.t("megafon.lore2", "<gray>tüm sunucu duysun!"),
                        Lang.t("megafon.lore3", "<dark_gray>Tek kullanımlık.")).hideAll().str(Keys.MEGAPHONE, "1").build();
    }

    private boolean isMegaphone(ItemStack it) { return ItemBuilder.pdcString(it, Keys.MEGAPHONE) != null; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMegaphone(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        long left = Cooldowns.left(p.getUniqueId(), "megaphone");
        if (left > 0) {
            Fx.fail(p);
            Lang.send(p, "megafon.bekleme", "<red>Megafon <sn> sn sonra kullanılabilir.", "sn", String.valueOf(left / 1000 + 1));
            return;
        }
        int max = cfg().getInt("megaphone.max-length", 100);
        Services.get(PromptService.class).ask(p, Lang.t("megafon.sor", "<yellow>Megafon mesajını yaz (en fazla <n> karakter):", "n", String.valueOf(max)), text -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!isMegaphone(hand)) { Fx.fail(p); Lang.send(p, "megafon.yok", "<red>Elinde megafon yok, iptal edildi."); return; }
            String msg = text.length() > max ? text.substring(0, max) : text;
            if (msg.isBlank()) return;
            hand.setAmount(hand.getAmount() - 1);                   // önce harca
            p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
            Cooldowns.set(p.getUniqueId(), "megaphone", cfg().getLong("megaphone.cooldown-seconds", 30) * 1000);
            broadcast(p, msg);
        }, null);
    }

    private void broadcast(Player p, String msg) {
        Io.log("megaphone", p.getName() + ": " + msg);
        Component chat = Lang.t("megafon.sohbet", "<gradient:#ff5555:#ffaa00>📢 <oyuncu>: <mesaj></gradient>", "oyuncu", p.getName(), "mesaj", msg);
        Title title = Title.title(Lang.t("megafon.baslik", "<red><bold>📢 <oyuncu>", "oyuncu", p.getName()),
                Lang.t("megafon.alt", "<gold><mesaj>", "mesaj", msg),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(600)));
        for (Player t : Bukkit.getOnlinePlayers()) {
            t.sendMessage(chat);
            t.showTitle(title);
            Fx.sound(t, Sound.BLOCK_BELL_USE, 1f, 1f);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
        long price = cfg().getLong("megaphone.price", 500);
        if (a.length > 0 && a[0].equalsIgnoreCase("al")) {
            EconomyService eco = Services.get(EconomyService.class);
            if (eco == null || !eco.take(p.getUniqueId(), price)) {
                Fx.fail(p);
                Lang.send(p, "megafon.altin-yok", "<red>Megafon <gold><fiyat> Altın<red>. Yeterli Altının yok.", "fiyat", Util.num(price));
                return true;
            }
            Util.give(p, item());
            Fx.ok(p);
            Lang.send(p, "megafon.alindi", "<green>Megafon satın alındı.");
        } else {
            Lang.send(p, "megafon.kullanim", "<gray>Megafon satın al: <white>/megafon al <gray>(<fiyat> Altın). Elinde tutup sağ tıkla.", "fiyat", Util.num(price));
        }
        return true;
    }
}
