package tr.sonumut.crate;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;
import java.util.Locale;

/** Kutu listesi ve yeni kutu oluşturma. */
final class CrateAdminMenu extends PagedMenu<CrateModule.Crate> {
    private final CrateModule mod;

    CrateAdminMenu(CrateModule mod) {
        super(Lang.t("gui.kutu-baslik", "<dark_gray>Kutular"), new ArrayList<>(mod.crates.values()));
        this.mod = mod;
    }

    @Override protected ItemStack icon(CrateModule.Crate c) {
        return ItemBuilder.of(Material.CHEST).name(Lang.mm(c.name)).lore("<gray>ID: <white>" + c.id, "<gray>Ödül: <white>" + c.rewards.size(),
                "<gray>Bağlı blok: <white>" + c.blocks.size(), " ", "<green>Düzenlemek için tıkla").hideAll().build();
    }

    @Override protected void clicked(Player p, CrateModule.Crate c, InventoryClickEvent e) {
        Fx.click(p);
        new CrateEditMenu(mod, c).open(p);
    }

    @Override protected void extra() {
        set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.kutu-yeni", "<green>Yeni kutu")).build(), (p, e) -> {
            Fx.click(p);
            Services.get(PromptService.class).ask(p, Lang.t("kutu.id-sor", "<yellow>Kutu ID'sini yaz (harf/rakam):"), text -> {
                String id = text.trim().toLowerCase(Locale.ROOT);
                if (!id.matches("[a-z0-9_]{1,16}") || mod.crates.containsKey(id)) {
                    Fx.fail(p);
                    Lang.send(p, "kutu.id-gecersiz", "<red>Geçersiz ya da kullanımda bir ID.");
                    new CrateAdminMenu(mod).open(p);
                    return;
                }
                mod.crates.put(id, new CrateModule.Crate(id));
                mod.save();
                new CrateEditMenu(mod, mod.crates.get(id)).open(p);
            }, () -> new CrateAdminMenu(mod).open(p));
        });
    }
}
