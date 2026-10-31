package tr.sonumut.quest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Görev yönetimi: oluştur (adım adım soru), sil, ödül eşyası ata. */
final class QuestAdminMenu extends PagedMenu<QuestModule.Def> {
    private static final String[] ASK = {
            "<yellow>Görev ID'si (harf/rakam/_):", "<yellow>Görev adı:", "<yellow>Tür: <white>KILL <gray>(mob), <white>MINE <gray>(blok), <white>BOSS",
            "<yellow>Hedef (mob türü, blok türü, boss id ya da ANY):", "<yellow>Adet:", "<yellow>Altın ödülü:", "<yellow>XP ödülü:", "<yellow>Minimum seviye:"};
    private final QuestModule mod;

    QuestAdminMenu(QuestModule mod) {
        super(Lang.t("gui.gorev-admin", "<dark_gray>Görev Yönetimi"), new ArrayList<>(mod.defs.values()));
        this.mod = mod;
    }

    @Override protected ItemStack icon(QuestModule.Def d) {
        return ItemBuilder.of(Material.BOOK).name(Lang.mm("<gold>" + d.name + " <dark_gray>(" + d.id + ")"))
                .lore("<gray>" + d.type + " → " + d.target + " x" + d.amount, "<gray>Altın: <white>" + d.gold + " <gray>XP: <white>" + d.xp + " <gray>Sv: <white>" + d.minLevel,
                        "<gray>Ödül eşyası: <white>" + (Util.isAir(d.item) ? "yok" : Util.pretty(d.item.getType().name())),
                        "<gray>Tekrarlanabilir: <white>" + (d.repeatable ? "evet" : "hayır"), " ",
                        "<green>Sol tık: elindeki eşyayı ödül yap", "<yellow>Sağ tık: tekrarlanabilirliği aç/kapat", "<red>Shift+Sağ tık: sil").hideAll().build();
    }

    @Override protected void clicked(Player p, QuestModule.Def d, InventoryClickEvent e) {
        Fx.click(p);
        if (e.getClick() == ClickType.SHIFT_RIGHT) mod.defs.remove(d.id);
        else if (e.getClick() == ClickType.RIGHT) d.repeatable = !d.repeatable;
        else {
            ItemStack h = p.getInventory().getItemInMainHand();
            d.item = Util.isAir(h) ? null : h.clone();
        }
        mod.save();
        items.clear();
        items.addAll(mod.defs.values());
        refresh();
    }

    @Override protected void extra() {
        set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.gorev-yeni", "<green>Yeni görev")).build(), (p, e) -> { Fx.click(p); ask(p, 0, new ArrayList<>()); });
    }

    private void ask(Player p, int i, List<String> ans) {
        if (i >= ASK.length) { finish(p, ans); return; }
        Services.get(PromptService.class).ask(p, Lang.mm(ASK[i]), text -> { ans.add(text.trim()); ask(p, i + 1, ans); }, () -> new QuestAdminMenu(mod).open(p));
    }

    private void finish(Player p, List<String> a) {
        String id = a.get(0).toLowerCase(Locale.ROOT);
        String type = a.get(2).toUpperCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_]{1,24}") || mod.defs.containsKey(id) || !List.of("KILL", "MINE", "BOSS").contains(type)) {
            Fx.fail(p);
            Lang.send(p, "gorev.gecersiz", "<red>Geçersiz ID/tür ya da ID kullanımda. Görev oluşturulmadı.");
            new QuestAdminMenu(mod).open(p);
            return;
        }
        QuestModule.Def d = new QuestModule.Def();
        d.id = id;
        d.name = a.get(1);
        d.type = type;
        d.target = a.get(3).toUpperCase(Locale.ROOT).equals("ANY") ? "ANY" : a.get(3);
        d.amount = Math.max(1, Util.parseInt(a.get(4), 10));
        d.gold = Math.max(0, Util.parseLong(a.get(5), 0));
        d.xp = Math.max(0, Util.parseLong(a.get(6), 0));
        d.minLevel = Math.max(1, Util.parseInt(a.get(7), 1));
        mod.defs.put(id, d);
        mod.save();
        Fx.ok(p);
        new QuestAdminMenu(mod).open(p);
    }
}
