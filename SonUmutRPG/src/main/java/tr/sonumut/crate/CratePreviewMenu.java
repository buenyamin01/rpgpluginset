package tr.sonumut.crate;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

/** Kutu ödül önizlemesi (şanslarla). */
final class CratePreviewMenu extends PagedMenu<CrateModule.Reward> {
    private final CrateModule.Crate crate;

    CratePreviewMenu(CrateModule mod, CrateModule.Crate c) {
        super(Lang.mm(c.name + " <dark_gray>Ödüller"), c.rewards);
        this.crate = c;
    }

    @Override protected ItemStack icon(CrateModule.Reward r) {
        double pct = r.weight / Math.max(0.0001, CrateModule.total(crate)) * 100;
        return ItemBuilder.from(CrateModule.rewardIcon(r)).lore(" ", "<gray>Şans: <white>" + String.format("%.2f", pct) + "%",
                "<gray>Nadirlik: " + CrateModule.rarityOf(crate, r).colored()).build();
    }

    @Override protected void clicked(Player p, CrateModule.Reward r, InventoryClickEvent e) {}
}
