package tr.sonumut.rpg;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.ConfirmMenu;
import tr.sonumut.gui.Menu;

/** Sınıf seçimi. card=true ise Sınıf Değiştirme Kartı harcanır. */
public final class ClassMenu extends Menu {
    private final ProfileModule mod;
    private final boolean card;

    public ClassMenu(ProfileModule mod, boolean card) {
        super(Lang.t("gui.sinif-baslik", "<gradient:#74b9ff:#a29bfe><bold>Sınıfını Seç"), 3);
        this.mod = mod;
        this.card = card;
    }

    private ItemStack icon(ClassType c) {
        ItemBuilder b = ItemBuilder.of(c.icon).name(Lang.mm("<bold>" + c.ikon + " " + c.gradient()))
                .lore(Lang.mm("<gray>" + c.aciklama), Lang.mm(" "))
                .lore(Lang.mm("<yellow>Yetenek: <white>" + c.yetenek + " <gray>(Shift + F)"))
                .lore(Lang.mm("<gray>Mana: <white>" + c.manaCost + " <gray>Bekleme: <white>" + c.cooldown + " sn"))
                .lore(Lang.mm(" "));
        for (CombatStat s : CombatStat.values()) {
            double v = c.base(s);
            if (v > 0) b.lore(Lang.mm("<gray>" + s.ad + ": <white>" + trim(v) + s.birim));
        }
        if (c.hpBonus != 0) b.lore(Lang.mm("<gray>Can: <white>" + (c.hpBonus > 0 ? "+" : "") + trim(c.hpBonus)));
        b.lore(Lang.mm(" ")).lore(Lang.mm("<green>Seçmek için tıkla"));
        return b.hideAll().build();
    }

    private static String trim(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }

    @Override protected void render() {
        border(Material.BLACK_STAINED_GLASS_PANE);
        ClassType[] all = ClassType.values();
        for (int i = 0; i < all.length; i++) {
            ClassType c = all[i];
            set(11 + i, icon(c), (p, e) -> {
                Fx.click(p);
                new ConfirmMenu(Lang.t("gui.sinif-onay", "<dark_gray>Bu sınıfı seçiyor musun?"), icon(c),
                        pl -> apply(pl, c), pl -> new ClassMenu(mod, card).open(pl)).open(p);
            });
        }
    }

    private void apply(Player p, ClassType c) {
        PlayerProfile pr = mod.get(p);
        if (card) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (ItemBuilder.pdcString(hand, Keys.CLASS_CARD) == null) { Fx.fail(p); Lang.send(p, "rpg.kart-yok", "<red>Elinde Sınıf Değiştirme Kartı yok."); return; }
            if (pr.cls() == c) { Fx.fail(p); Lang.send(p, "rpg.ayni-sinif", "<red>Zaten bu sınıftasın."); return; }
            hand.setAmount(hand.getAmount() - 1);
            p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        } else if (pr.cls() != null) {
            Fx.fail(p);
            Lang.send(p, "rpg.sinif-kalici", "<gray>Sınıfın kalıcıdır. Değiştirmek için <white>Sınıf Değiştirme Kartı <gray>gerekir.");
            return;
        }
        mod.setClass(p, c);
    }
}
