package tr.sonumut.loot;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;
import java.util.Locale;

/** Mob tablolarının listesi. */
final class LootAdminMenu extends PagedMenu<String> {
    private final LootModule mod;

    LootAdminMenu(LootModule mod) {
        super(Lang.t("gui.loot-baslik", "<dark_gray>Ganimet Tabloları"), new ArrayList<>(mod.tables.keySet()));
        this.mod = mod;
    }

    @Override protected ItemStack icon(String key) {
        Material m = Material.matchMaterial(key + "_SPAWN_EGG");
        if (m == null) m = key.equals("ALL") ? Material.ENDER_CHEST : Material.CHEST;
        int n = mod.tables.getOrDefault(key, java.util.List.of()).size();
        return ItemBuilder.of(m).name(Lang.mm("<yellow>" + (key.equals("ALL") ? "Tüm mobler" : Util.pretty(key))))
                .lore("<gray>Kayıt sayısı: <white>" + n, " ", "<green>Düzenlemek için tıkla").hideAll().build();
    }

    @Override protected void clicked(Player p, String key, InventoryClickEvent e) {
        Fx.click(p);
        new LootTableMenu(mod, key).open(p);
    }

    @Override protected void extra() {
        set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.loot-yeni", "<green>Yeni mob tablosu"))
                .lore("<gray>Mob türünü yaz (ör. ZOMBIE) ya da", "<gray>her mob için <white>ALL").build(), (p, e) -> {
            Fx.click(p);
            Services.get(PromptService.class).ask(p, Lang.t("loot.mob-sor", "<yellow>Mob türünü yaz (ör. ZOMBIE veya ALL):"), text -> {
                String key = text.trim().toUpperCase(Locale.ROOT);
                boolean ok = key.equals("ALL");
                if (!ok) {
                    try { ok = EntityType.valueOf(key).isAlive(); } catch (IllegalArgumentException ignored) { }
                }
                if (!ok) { Fx.fail(p); Lang.send(p, "loot.gecersiz", "<red>Geçersiz mob türü."); new LootAdminMenu(mod).open(p); return; }
                mod.tables.computeIfAbsent(key, k -> new ArrayList<>());
                new LootTableMenu(mod, key).open(p);
            }, () -> new LootAdminMenu(mod).open(p));
        });
    }
}
