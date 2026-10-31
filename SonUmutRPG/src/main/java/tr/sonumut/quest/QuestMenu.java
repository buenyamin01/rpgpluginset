package tr.sonumut.quest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;

/** Oyuncu görev listesi: tıkla = al, Shift+Sağ tık = bırak. */
final class QuestMenu extends PagedMenu<QuestModule.Def> {
    private final QuestModule mod;

    QuestMenu(QuestModule mod) {
        super(Lang.t("gui.gorev-baslik", "<gradient:#7bed9f:#2ed573><bold>Görevler"), new ArrayList<>(mod.defs.values()));
        this.mod = mod;
    }

    @Override protected ItemStack icon(QuestModule.Def d) {
        Player p = viewer;
        int prog = mod.progressOf(p, d.id);
        long lock = mod.lockLeft(p, d);
        Material m;
        String state;
        if (prog >= 0) { m = Material.WRITABLE_BOOK; state = "<yellow>Aktif"; }
        else if (lock < 0) { m = Material.BOOK; state = "<gray>Tamamlandı"; }
        else if (lock > 0) { m = Material.CLOCK; state = "<gray>Bekleniyor: " + (lock / 3600000 + 1) + " sa"; }
        else if (mod.ps().get(p).level() < d.minLevel) { m = Material.BARRIER; state = "<red>Seviye " + d.minLevel + " gerekir"; }
        else { m = Material.ENCHANTED_BOOK; state = "<green>Alınabilir"; }
        ItemBuilder b = ItemBuilder.of(m).name(Lang.mm("<gold>" + d.name)).lore("<dark_gray>" + d.type + " → " + d.target + " x" + d.amount);
        if (!d.desc.isEmpty()) b.lore("<gray>" + d.desc);
        b.lore(" ", "<gray>Durum: " + state);
        if (prog >= 0) b.lore("<gray>İlerleme: <white>" + prog + "/" + d.amount, Util.bar((double) prog / d.amount, 20));
        b.lore(" ", "<gray>Ödül: <gold>" + Util.num(d.gold) + " Altın <gray>+ <aqua>" + Util.num(d.xp) + " XP");
        if (prog >= 0) b.lore("<red>Shift+Sağ tık: bırak"); else if (lock == 0) b.lore("<green>Tıkla: görevi al");
        return b.hideAll().build();
    }

    @Override protected void clicked(Player p, QuestModule.Def d, InventoryClickEvent e) {
        if (mod.isActive(p, d.id)) { if (e.getClick() == ClickType.SHIFT_RIGHT) mod.abandon(p, d); }
        else mod.accept(p, d);
        refresh();
    }
}
