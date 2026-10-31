package tr.sonumut.boss;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Boss listesi / oluşturma. */
final class BossAdminMenu extends PagedMenu<BossModule.Def> {
    private static final String[] ASK = {
            "<yellow>Boss ID'si (harf/rakam/_):", "<yellow>Boss adı (MiniMessage renkleri geçerli):", "<yellow>Mob türü (ör. ZOMBIE, WITHER_SKELETON):",
            "<yellow>Can:", "<yellow>Hasar:", "<yellow>Seviye:", "<yellow>XP ödülü:", "<yellow>Altın ödülü:", "<yellow>Yeniden doğma (dakika, 0 = yok):"};
    private final BossModule mod;

    BossAdminMenu(BossModule mod) {
        super(Lang.t("gui.boss-baslik", "<dark_red>Bosslar"), new ArrayList<>(mod.defs.values()));
        this.mod = mod;
    }

    @Override protected ItemStack icon(BossModule.Def d) {
        return ItemBuilder.of(Material.NETHER_STAR).name(Lang.mm(d.name + " <dark_gray>(" + d.id + ")"))
                .lore("<gray>Tür: <white>" + Util.pretty(d.type.name()), "<gray>Can: <white>" + d.health + " <gray>Hasar: <white>" + d.damage + " <gray>Sv: <white>" + d.level,
                        "<gray>XP: <white>" + d.xp + " <gray>Altın: <white>" + d.gold, "<gray>Yeniden doğma: <white>" + d.respawn + " dk",
                        "<gray>Doğma noktası: <white>" + (d.spawn == null ? "yok" : d.spawn), "<gray>Düşmeler: <white>" + d.drops.size(),
                        "<gray>Durum: " + (d.alive ? "<green>yaşıyor" : "<red>ölü/bekliyor"), " ",
                        "<green>Sol tık: şimdi doğur", "<yellow>Sağ tık: doğma noktası = bulunduğun yer", "<aqua>Shift+Sol: canlıyı kaldır/durumu sıfırla", "<red>Shift+Sağ: sil").hideAll().build();
    }

    @Override protected void clicked(Player p, BossModule.Def d, InventoryClickEvent e) {
        ClickType c = e.getClick();
        Fx.click(p);
        if (c == ClickType.SHIFT_RIGHT) mod.delete(d);
        else if (c == ClickType.SHIFT_LEFT) mod.resetAlive(d);
        else if (c == ClickType.RIGHT) { d.spawn = Util.locStr(p.getLocation()); mod.save(); }
        else { p.closeInventory(); mod.spawnNow(p, d); return; }
        items.clear();
        items.addAll(mod.defs.values());
        refresh();
    }

    @Override protected void extra() {
        set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.boss-yeni", "<green>Yeni boss")).build(), (p, e) -> { Fx.click(p); ask(p, 0, new ArrayList<>()); });
    }

    private void ask(Player p, int i, List<String> a) {
        if (i >= ASK.length) { finish(p, a); return; }
        Services.get(PromptService.class).ask(p, Lang.mm(ASK[i]), t -> { a.add(t.trim()); ask(p, i + 1, a); }, () -> new BossAdminMenu(mod).open(p));
    }

    private void finish(Player p, List<String> a) {
        String id = a.get(0).toLowerCase(Locale.ROOT);
        EntityType type = null;
        try { type = EntityType.valueOf(a.get(2).toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { }
        if (!id.matches("[a-z0-9_]{1,24}") || mod.defs.containsKey(id) || type == null || !type.isAlive() || !type.isSpawnable()) {
            Fx.fail(p);
            Lang.send(p, "boss.gecersiz", "<red>Geçersiz ID/mob türü ya da ID kullanımda. Boss oluşturulmadı.");
            new BossAdminMenu(mod).open(p);
            return;
        }
        BossModule.Def d = new BossModule.Def();
        d.id = id;
        d.name = a.get(1);
        d.type = type;
        d.health = Math.max(1, Util.parseDouble(a.get(3), 300));
        d.damage = Math.max(0, Util.parseDouble(a.get(4), 8));
        d.level = Math.max(1, Util.parseInt(a.get(5), 10));
        d.xp = Math.max(0, Util.parseLong(a.get(6), 300));
        d.gold = Math.max(0, Util.parseLong(a.get(7), 200));
        d.respawn = Math.max(0, Util.parseInt(a.get(8), 30));
        d.spawn = Util.locStr(p.getLocation());
        mod.defs.put(id, d);
        mod.save();
        Fx.ok(p);
        Lang.send(p, "boss.olusturuldu", "<green>Boss oluşturuldu. Doğma noktası bulunduğun yer. Düşme eklemek için: <white>/sonumut boss drop <id> <şans>");
        new BossAdminMenu(mod).open(p);
    }
}
