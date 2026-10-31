package tr.sonumut.trade;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.Menu;

/** Sol 4x4 alan: kendi teklifin (serbest). Sağ 4x4 alan: karşı tarafın teklifi (yalnızca görüntü). */
final class TradeMenu extends Menu {
    private final TradeSession s;
    private final boolean mineA;

    TradeMenu(TradeSession s, boolean mineA) {
        super(Lang.t("gui.takas-baslik", "<dark_green>Takas"), 6);
        this.s = s;
        this.mineA = mineA;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) freeSlot(r * 9 + c);
    }

    void clearSlot(int slot) { inv.setItem(slot, null); }

    @Override protected void render() {
        fill(Material.BLACK_STAINED_GLASS_PANE);
        for (int r = 0; r < 6; r++) set(r * 9 + 4, pane(Material.GRAY_STAINED_GLASS_PANE));
        TradeMenu other = mineA ? s.mb : s.ma;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                ItemStack it = other.inv.getItem(r * 9 + c);
                set(r * 9 + c + 5, Util.isAir(it) ? null : it.clone());
            }
        }
        long mine = mineA ? s.goldA : s.goldB, theirs = mineA ? s.goldB : s.goldA;
        boolean myReady = mineA ? s.readyA : s.readyB, theirReady = mineA ? s.readyB : s.readyA;
        set(36, ItemBuilder.of(Material.GOLD_INGOT).name(Lang.mm("<gold>Teklifin: " + Util.num(mine) + " Altın"))
                .lore("<gray>Değiştirmek için: <white>/takas altin <miktar>").build(),
                (p, e) -> Lang.send(p, "takas.altin-ipucu", "<gray>Altın teklifi için: <white>/takas altin <miktar>"));
        set(44, ItemBuilder.of(Material.GOLD_NUGGET).name(Lang.mm("<gold>Karşı teklif: " + Util.num(theirs) + " Altın")).build());
        set(40, ItemBuilder.of(theirReady ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(Lang.mm(theirReady ? "<green>Karşı taraf hazır" : "<gray>Karşı taraf hazır değil")).build());
        set(49, ItemBuilder.of(myReady ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE)
                .name(Lang.mm(myReady ? "<green><bold>HAZIRSIN" : "<yellow><bold>HAZIRIM"))
                .lore("<gray>İki taraf hazır olunca 3 sn sonra tamamlanır.", "<gray>Herhangi bir değişiklik onayı sıfırlar.").build(),
                (p, e) -> s.toggleReady(p));
        set(53, ItemBuilder.of(Material.RED_CONCRETE).name(Lang.t("gui.takas-iptal", "<red><bold>İPTAL")).build(), (p, e) -> p.closeInventory());
    }

    @Override protected void onFreeChange(Player p) { s.changed(); }

    @Override protected void onClosed(Player p) { if (!s.done) s.cancel(p); }
}
