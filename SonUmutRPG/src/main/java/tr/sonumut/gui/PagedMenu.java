package tr.sonumut.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.Fx;
import tr.sonumut.core.ItemBuilder;
import tr.sonumut.core.Lang;

import java.util.List;

/** Sayfalı liste menüsü: 45 içerik slotu + alt satır gezinme. */
public abstract class PagedMenu<T> extends Menu {
    protected final List<T> items;
    protected int page;

    protected PagedMenu(Component title, List<T> items) {
        super(title, 6);
        this.items = items;
    }

    protected abstract ItemStack icon(T t);

    protected abstract void clicked(Player p, T t, InventoryClickEvent e);

    /** Alt satırdaki 46, 47, 50, 51, 52 slotlarına ek düğmeler koymak için. */
    protected void extra() {}

    @Override protected void render() {
        int max = Math.max(0, (items.size() - 1) / 45);
        if (page > max) page = max;
        int start = page * 45;
        for (int i = 0; i < 45 && start + i < items.size(); i++) {
            T t = items.get(start + i);
            set(i, icon(t), (p, e) -> clicked(p, t, e));
        }
        for (int i = 45; i < 54; i++) set(i, pane(Material.BLACK_STAINED_GLASS_PANE));
        if (page > 0) set(45, ItemBuilder.of(Material.ARROW).name(Lang.t("gui.onceki", "<yellow>« Önceki sayfa")).build(),
                (p, e) -> { page--; Fx.click(p); refresh(); });
        if (page < max) set(53, ItemBuilder.of(Material.ARROW).name(Lang.t("gui.sonraki", "<yellow>Sonraki sayfa »")).build(),
                (p, e) -> { page++; Fx.click(p); refresh(); });
        set(49, ItemBuilder.of(Material.PAPER).name(Lang.t("gui.sayfa", "<gray>Sayfa <white><sayfa></white>/<white><toplam></white>",
                "sayfa", String.valueOf(page + 1), "toplam", String.valueOf(max + 1))).build());
        extra();
    }
}
