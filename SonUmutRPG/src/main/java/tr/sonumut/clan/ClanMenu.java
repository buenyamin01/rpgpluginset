package tr.sonumut.clan;

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

/** Klan üyeleri, banka ve yönetim. */
final class ClanMenu extends PagedMenu<UUID> {
    private final ClanModule mod;

    ClanMenu(ClanModule mod) {
        super(Lang.t("gui.klan-baslik", "<gold><bold>Klan"), new ArrayList<>());
        this.mod = mod;
    }

    private Clan clan() { return viewer == null ? null : mod.of(viewer.getUniqueId()).orElse(null); }

    @Override protected void render() {
        items.clear();
        Clan c = clan();
        if (c != null) items.addAll(c.members().keySet());
        super.render();
    }

    @Override protected ItemStack icon(UUID id) {
        Clan c = clan();
        Clan.Role r = c.roleOf(id);
        ItemBuilder b = ItemBuilder.of(Material.PLAYER_HEAD).owner(Bukkit.getOfflinePlayer(id))
                .name(Lang.mm((r == Clan.Role.LIDER ? "<gold>★ " : r == Clan.Role.YONETICI ? "<yellow>" : "<aqua>") + ClanModule.nameOf(id)))
                .lore("<gray>Rütbe: <white>" + r.ad, Bukkit.getPlayer(id) != null ? "<green>Çevrimiçi" : "<red>Çevrimdışı");
        Clan.Role me = c.roleOf(viewer.getUniqueId());
        if (me.manages() && !id.equals(viewer.getUniqueId())) b.lore(" ", "<red>Shift+Sağ tık: klandan at");
        if (me == Clan.Role.LIDER && !id.equals(viewer.getUniqueId())) {
            if (r == Clan.Role.UYE) b.lore("<green>Shift+Sol tık: yönetici yap");
            else b.lore("<green>Shift+Sol tık: liderliği devret", "<yellow>Sol tık: üyeliğe indir");
        }
        return b.build();
    }

    @Override protected void clicked(Player p, UUID id, InventoryClickEvent e) {
        ClickType t = e.getClick();
        if (t == ClickType.SHIFT_RIGHT) mod.kick(p, id);
        else if (t == ClickType.SHIFT_LEFT) mod.promote(p, id);
        else if (t == ClickType.LEFT) mod.demote(p, id);
        refresh();
    }

    @Override protected void extra() {
        Clan c = clan();
        PromptService ps = Services.get(PromptService.class);
        if (c == null) {
            set(47, ItemBuilder.of(Material.EMERALD).name(Lang.t("gui.klan-kur", "<green>Klan kur"))
                    .lore("<gray>Ücret: <gold>" + Util.num(mod.cfgCost()) + " Altın").build(), (p, e) -> {
                Fx.click(p);
                ps.ask(p, Lang.t("klan.tag-sor", "<yellow>Klan etiketini yaz (2-4 harf/rakam):"), tag ->
                        ps.ask(p, Lang.t("klan.ad-sor", "<yellow>Klan adını yaz:"), name -> {
                            mod.create(p, tag.trim(), name.trim());
                            new ClanMenu(mod).open(p);
                        }, () -> new ClanMenu(mod).open(p)), () -> new ClanMenu(mod).open(p));
            });
            return;
        }
        set(47, ItemBuilder.of(Material.GOLD_BLOCK).name(Lang.mm("<gold>Banka: " + Util.num(c.bank()) + " Altın"))
                .lore("<green>Sol tık: yatır", "<yellow>Sağ tık: çek (yönetici)").build(), (p, e) -> {
            boolean dep = e.getClick() == ClickType.LEFT;
            ps.ask(p, Lang.t("klan.miktar-sor", "<yellow>Miktarı yaz:"), text -> {
                long n = Util.parseLong(text, 0);
                if (dep) mod.deposit(p, n); else mod.withdraw(p, n);
                new ClanMenu(mod).open(p);
            }, () -> new ClanMenu(mod).open(p));
        });
        if (c.roleOf(viewer.getUniqueId()).manages()) {
            set(48, ItemBuilder.of(Material.WRITABLE_BOOK).name(Lang.t("gui.klan-davet", "<green>Oyuncu davet et")).build(), (p, e) ->
                    ps.ask(p, Lang.t("klan.davet-sor", "<yellow>Davet edilecek oyuncunun adını yaz:"), text -> {
                        Player t = Bukkit.getPlayerExact(text.trim());
                        if (t == null) Lang.send(p, "klan.oyuncu-yok", "<red>Oyuncu bulunamadı."); else mod.invite(p, t);
                        new ClanMenu(mod).open(p);
                    }, () -> new ClanMenu(mod).open(p)));
        }
        set(50, ItemBuilder.of(Material.NAME_TAG).name(Lang.mm("<gold>[" + c.tag() + "] <yellow>" + c.name()))
                .lore("<gray>Üye: <white>" + c.members().size()).build());
        if (!c.leader().equals(viewer.getUniqueId())) {
            set(51, ItemBuilder.of(Material.BARRIER).name(Lang.t("gui.klan-ayril", "<red>Klandan ayrıl")).build(),
                    (p, e) -> { Fx.click(p); mod.leave(p); refresh(); });
        }
    }
}
