package tr.sonumut.party;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;
import tr.sonumut.gui.PagedMenu;

import java.util.ArrayList;
import java.util.UUID;

/** Parti üyeleri; lider Shift+Sağ tıkla üye atabilir. */
final class PartyMenu extends PagedMenu<UUID> {
    private final PartyModule mod;

    PartyMenu(PartyModule mod) {
        super(Lang.t("gui.parti-baslik", "<dark_aqua><bold>Parti"), new ArrayList<>());
        this.mod = mod;
    }

    @Override protected void render() {
        items.clear();
        Party pt = viewer != null ? mod.of(viewer.getUniqueId()).orElse(null) : null;
        if (pt != null) items.addAll(pt.members());
        super.render();
    }

    @Override protected ItemStack icon(UUID id) {
        Party pt = mod.of(viewer.getUniqueId()).orElse(null);
        boolean leader = pt != null && pt.leader().equals(id);
        Player online = Bukkit.getPlayer(id);
        ItemBuilder b = ItemBuilder.of(Material.PLAYER_HEAD).owner(Bukkit.getOfflinePlayer(id))
                .name(Lang.mm((leader ? "<gold>★ " : "<aqua>") + PartyModule.nameOf(id)))
                .lore(online != null ? "<green>Çevrimiçi" : "<red>Çevrimdışı");
        if (pt != null && pt.leader().equals(viewer.getUniqueId()) && !id.equals(viewer.getUniqueId()))
            b.lore(" ", "<red>Shift+Sağ tık: partiden at");
        return b.build();
    }

    @Override protected void clicked(Player p, UUID id, InventoryClickEvent e) {
        if (e.getClick() == ClickType.SHIFT_RIGHT) { mod.kick(p, id); refresh(); }
    }

    @Override protected void extra() {
        boolean in = mod.of(viewer.getUniqueId()).isPresent();
        if (!in) {
            set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.parti-kur", "<green>Parti kur")).build(),
                    (p, e) -> { Fx.ok(p); mod.create(p); refresh(); });
            return;
        }
        set(47, ItemBuilder.of(Material.WRITABLE_BOOK).name(Lang.t("gui.parti-davet", "<green>Oyuncu davet et")).build(), (p, e) -> {
            Fx.click(p);
            Services.get(PromptService.class).ask(p, Lang.t("parti.davet-sor", "<yellow>Davet edilecek oyuncunun adını yaz:"), text -> {
                Player t = Bukkit.getPlayerExact(text.trim());
                if (t == null) Lang.send(p, "parti.oyuncu-yok", "<red>Oyuncu bulunamadı."); else mod.invite(p, t);
                new PartyMenu(mod).open(p);
            }, () -> new PartyMenu(mod).open(p));
        });
        set(51, ItemBuilder.of(Material.BARRIER).name(Lang.t("gui.parti-ayril", "<red>Partiden ayrıl")).build(),
                (p, e) -> { Fx.click(p); mod.leave(p.getUniqueId(), false); refresh(); });
    }
}
