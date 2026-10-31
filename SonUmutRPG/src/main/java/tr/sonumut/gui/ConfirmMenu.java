package tr.sonumut.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.Fx;
import tr.sonumut.core.ItemBuilder;
import tr.sonumut.core.Lang;

import java.util.function.Consumer;

/** Evet / Hayır onay menüsü. */
public final class ConfirmMenu extends Menu {
    private final ItemStack info;
    private final Consumer<Player> yes;
    private final Consumer<Player> no;

    public ConfirmMenu(Component title, ItemStack info, Consumer<Player> yes, Consumer<Player> no) {
        super(title, 3);
        this.info = info;
        this.yes = yes;
        this.no = no;
    }

    @Override protected void render() {
        border(Material.BLACK_STAINED_GLASS_PANE);
        set(13, info);
        set(11, ItemBuilder.of(Material.LIME_CONCRETE).name(Lang.t("gui.evet", "<green><bold>EVET")).build(),
                (p, e) -> { Fx.ok(p); p.closeInventory(); yes.accept(p); });
        set(15, ItemBuilder.of(Material.RED_CONCRETE).name(Lang.t("gui.hayir", "<red><bold>HAYIR")).build(),
                (p, e) -> { Fx.click(p); p.closeInventory(); if (no != null) no.accept(p); });
    }
}
