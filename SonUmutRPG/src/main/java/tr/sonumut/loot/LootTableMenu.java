package tr.sonumut.loot;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

/** Tek bir mobun ganimet kayıtları. */
final class LootTableMenu extends PagedMenu<LootModule.Entry> {
    private final LootModule mod;
    private final String key;

    LootTableMenu(LootModule mod, String key) {
        super(Lang.t("gui.loot-tablo", "<dark_gray>Ganimet: <yellow><mob>", "mob", key.equals("ALL") ? "Tüm mobler" : Util.pretty(key)),
                mod.tables.computeIfAbsent(key, k -> new java.util.ArrayList<>()));
        this.mod = mod;
        this.key = key;
    }

    @Override protected ItemStack icon(LootModule.Entry e) {
        return ItemBuilder.from(e.item)
                .lore(" ", "<gray>Şans: <white>" + e.chance + "%", "<gray>Adet: <white>" + e.min + "-" + e.max,
                        "<gray>Min. mob seviyesi: <white>" + e.minLevel, " ",
                        "<green>Sol tık: şansı değiştir", "<green>Sağ tık: adet ve seviye", "<red>Shift+Sağ tık: sil").build();
    }

    private void back(Player p) { new LootTableMenu(mod, key).open(p); }

    @Override protected void clicked(Player p, LootModule.Entry en, InventoryClickEvent e) {
        PromptService ps = Services.get(PromptService.class);
        ClickType c = e.getClick();
        Fx.click(p);
        if (c == ClickType.SHIFT_RIGHT) {
            items.remove(en);
            mod.save();
            refresh();
        } else if (c == ClickType.RIGHT) {
            ps.ask(p, Lang.t("loot.adet-sor", "<yellow>Şunu yaz: <white><min> <max> <minSeviye> <yellow>(ör. 1 3 10)"), text -> {
                String[] a = text.trim().split("\\s+");
                if (a.length < 2) { Fx.fail(p); back(p); return; }
                en.min = Math.max(1, Util.parseInt(a[0], 1));
                en.max = Math.max(en.min, Util.parseInt(a[1], en.min));
                en.minLevel = a.length > 2 ? Math.max(1, Util.parseInt(a[2], 1)) : 1;
                mod.save();
                Fx.ok(p);
                back(p);
            }, () -> back(p));
        } else {
            ps.ask(p, Lang.t("loot.sans-sor", "<yellow>Düşme şansını yaz (% olarak, ör. 12.5):"), text -> {
                en.chance = Util.clamp(Util.parseDouble(text, en.chance), 0.001, 100);
                mod.save();
                Fx.ok(p);
                back(p);
            }, () -> back(p));
        }
    }

    @Override protected void extra() {
        set(47, ItemBuilder.of(Material.HOPPER).name(Lang.t("gui.loot-ekle", "<green>Elindeki eşyayı ekle"))
                .lore("<gray>Ana elindeki eşya (adı ve özellikleriyle)", "<gray>tabloya eklenir.").build(), (p, e) -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (Util.isAir(hand)) { Fx.fail(p); Lang.send(p, "loot.el-bos", "<red>Ana elinde eşya yok."); return; }
            ItemStack copy = hand.clone();
            copy.setAmount(1);
            Services.get(PromptService.class).ask(p, Lang.t("loot.sans-sor", "<yellow>Düşme şansını yaz (% olarak, ör. 12.5):"), text -> {
                LootModule.Entry en = new LootModule.Entry();
                en.item = copy;
                en.chance = Util.clamp(Util.parseDouble(text, 10), 0.001, 100);
                items.add(en);
                mod.save();
                Fx.ok(p);
                back(p);
            }, () -> back(p));
        });
        set(48, ItemBuilder.of(Material.BARRIER).name(Lang.t("gui.geri", "<red>Geri")).build(),
                (p, e) -> { Fx.click(p); new LootAdminMenu(mod).open(p); });
    }
}
