package tr.sonumut.rpg;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tr.sonumut.core.*;
import tr.sonumut.gui.Menu;

/** Profil ve yetenek puanı dağıtımı. */
public final class ProfileMenu extends Menu {
    private final ProfileModule mod;

    public ProfileMenu(ProfileModule mod) {
        super(Lang.t("gui.profil-baslik", "<gradient:#ffd27f:#ff7e5f><bold>Profil"), 4);
        this.mod = mod;
    }

    @Override protected void render() {
        Player p = viewer;
        PlayerProfile pr = mod.get(p);
        border(Material.BLACK_STAINED_GLASS_PANE);
        long need = mod.xpNeeded(pr.level());
        ClassType c = pr.cls();
        EconomyService eco = Services.get(EconomyService.class);
        ItemBuilder head = ItemBuilder.of(Material.PLAYER_HEAD).owner(p)
                .name(Lang.mm("<yellow>" + p.getName()))
                .lore(Lang.mm("<gray>Sınıf: " + (c == null ? "<red>Seçilmedi" : c.gradient())),
                        Lang.mm("<gray>Seviye: <white>" + pr.level()),
                        Lang.mm("<gray>XP: <white>" + Util.num(pr.xp()) + "<gray>/<white>" + Util.num(need)),
                        Lang.mm(Util.bar((double) pr.xp() / need, 20)),
                        Lang.mm("<gray>Öldürme: <white>" + Util.num(pr.kills())),
                        Lang.mm("<gray>Mana: <aqua>" + (int) mod.mana(p) + "<gray>/<aqua>" + (int) mod.maxMana(p)));
        if (eco != null) head.lore(Lang.mm("<gray>Altın: <gold>" + Util.num(eco.gold(p.getUniqueId()))));
        head.lore(Lang.mm(" ")).lore(Lang.mm("<yellow>Yetenek puanı: <white>" + pr.points()));
        set(4, head.build());
        int slot = 19;
        for (CombatStat s : CombatStat.values()) {
            double total = mod.combatStat(p, s);
            set(slot++, ItemBuilder.of(s.icon).name(Lang.mm("<aqua>" + s.ad))
                    .lore(Lang.mm("<gray>Toplam: <white>" + String.format("%.1f", total) + s.birim + " <dark_gray>(sınır " + (int) s.cap + ")"),
                            Lang.mm("<gray>Dağıtılan puan: <white>" + pr.alloc(s)),
                            Lang.mm("<gray>Puan başı: <white>+" + s.perPoint + s.birim),
                            Lang.mm(" "),
                            Lang.mm("<green>Sol tık: +1 puan <dark_gray>| <green>Shift+Sol: +5")).hideAll().build(),
                    (pl, e) -> {
                        int n = e.getClick() == ClickType.SHIFT_LEFT ? 5 : 1;
                        n = Math.min(n, pr.points());
                        if (n <= 0 || total >= s.cap) { Fx.fail(pl); return; }
                        pr.addAlloc(s, n);
                        pr.addPoints(-n);
                        pr.save();
                        Fx.ok(pl);
                        refresh();
                    });
        }
    }
}
