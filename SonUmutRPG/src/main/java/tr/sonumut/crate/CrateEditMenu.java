package tr.sonumut.crate;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

/** Tek bir kutunun ödüllerini düzenler. */
final class CrateEditMenu extends PagedMenu<CrateModule.Reward> {
    private final CrateModule mod;
    private final CrateModule.Crate crate;

    CrateEditMenu(CrateModule mod, CrateModule.Crate c) {
        super(Lang.mm(c.name + " <dark_gray>Düzenle"), c.rewards);
        this.mod = mod;
        this.crate = c;
    }

    private void back(Player p) { new CrateEditMenu(mod, crate).open(p); }

    @Override protected ItemStack icon(CrateModule.Reward r) {
        double pct = r.weight / Math.max(0.0001, CrateModule.total(crate)) * 100;
        return ItemBuilder.from(CrateModule.rewardIcon(r)).lore(" ", "<gray>Ağırlık: <white>" + r.weight + " <dark_gray>(" + String.format("%.2f", pct) + "%)",
                "<gray>Nadirlik: " + CrateModule.rarityOf(crate, r).colored(), " ", "<green>Sol tık: ağırlığı değiştir", "<red>Shift+Sağ tık: sil").build();
    }

    @Override protected void clicked(Player p, CrateModule.Reward r, InventoryClickEvent e) {
        Fx.click(p);
        if (e.getClick() == ClickType.SHIFT_RIGHT) { items.remove(r); mod.save(); refresh(); return; }
        Services.get(PromptService.class).ask(p, Lang.t("kutu.agirlik-sor", "<yellow>Yeni ağırlığı yaz (ör. 10):"), text -> {
            r.weight = Math.max(0.0001, Util.parseDouble(text, r.weight));
            mod.save();
            back(p);
        }, () -> back(p));
    }

    @Override protected void extra() {
        PromptService ps = Services.get(PromptService.class);
        set(46, ItemBuilder.of(Material.HOPPER).name(Lang.t("gui.kutu-esya", "<green>Elindeki eşyayı ekle")).build(), (p, e) -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (Util.isAir(hand)) { Fx.fail(p); Lang.send(p, "kutu.el-bos", "<red>Ana elinde eşya yok."); return; }
            ItemStack copy = hand.clone();
            ps.ask(p, Lang.t("kutu.agirlik-sor", "<yellow>Yeni ağırlığı yaz (ör. 10):"), text -> {
                CrateModule.Reward r = new CrateModule.Reward();
                r.item = copy;
                r.weight = Math.max(0.0001, Util.parseDouble(text, 10));
                items.add(r);
                mod.save();
                back(p);
            }, () -> back(p));
        });
        set(47, ItemBuilder.of(Material.GOLD_INGOT).name(Lang.t("gui.kutu-altin", "<gold>Altın ödülü ekle")).build(), (p, e) ->
                ps.ask(p, Lang.t("kutu.altin-sor", "<yellow>Şunu yaz: <white><miktar> <agirlik> <yellow>(ör. 500 20)"), text -> {
                    String[] a = text.trim().split("\\s+");
                    long g = Util.parseLong(a[0], 0);
                    if (g <= 0) { Fx.fail(p); back(p); return; }
                    CrateModule.Reward r = new CrateModule.Reward();
                    r.gold = g;
                    r.weight = a.length > 1 ? Math.max(0.0001, Util.parseDouble(a[1], 10)) : 10;
                    items.add(r);
                    mod.save();
                    back(p);
                }, () -> back(p)));
        set(48, ItemBuilder.of(Material.NAME_TAG).name(Lang.t("gui.kutu-ad", "<yellow>Adı değiştir")).build(), (p, e) ->
                ps.ask(p, Lang.t("kutu.ad-sor", "<yellow>Yeni adı yaz (MiniMessage renkleri geçerli):"), text -> {
                    crate.name = text;
                    mod.save();
                    back(p);
                }, () -> back(p)));
        set(50, ItemBuilder.of(Material.TRIPWIRE_HOOK).name(Lang.t("gui.kutu-anahtar", "<aqua>Anahtar al")).build(), (p, e) -> {
            Util.give(p, mod.keyItem(crate));
            Fx.ok(p);
        });
        set(51, ItemBuilder.of(Material.BARRIER).name(Lang.t("gui.geri", "<red>Geri")).build(), (p, e) -> new CrateAdminMenu(mod).open(p));
    }
}
