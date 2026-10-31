package tr.sonumut.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    private ItemBuilder(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
        List<Component> l = meta.lore();
        if (l != null) lore.addAll(l);
    }

    public static ItemBuilder of(Material m) { return new ItemBuilder(new ItemStack(m)); }

    public static ItemBuilder of(Material m, int amount) { return new ItemBuilder(new ItemStack(m, Math.max(1, amount))); }

    public static ItemBuilder from(ItemStack s) { return new ItemBuilder(s.clone()); }

    private static Component noItalic(Component c) { return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE); }

    public ItemBuilder name(String mm) { return name(Lang.mm(mm)); }

    public ItemBuilder name(Component c) { meta.displayName(noItalic(c)); return this; }

    public ItemBuilder lore(String... lines) {
        for (String l : lines) lore.add(noItalic(Lang.mm(l)));
        return this;
    }

    public ItemBuilder lore(Component... cs) { for (Component c : cs) lore.add(noItalic(c)); return this; }

    public ItemBuilder clearLore() { lore.clear(); return this; }

    public ItemBuilder glow(boolean g) { meta.setEnchantmentGlintOverride(g ? Boolean.TRUE : null); return this; }

    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        return this;
    }

    public ItemBuilder str(NamespacedKey k, String v) { meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, v); return this; }

    public ItemBuilder integer(NamespacedKey k, int v) { meta.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, v); return this; }

    public ItemBuilder owner(OfflinePlayer p) { if (meta instanceof SkullMeta sm) sm.setOwningPlayer(p); return this; }

    public ItemBuilder amount(int a) { item.setAmount(Math.max(1, a)); return this; }

    public ItemStack build() {
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Hazır eşyaya (kopya) eklenen lore satırlarıyla yeni bir eşya. */
    public static ItemStack withLore(ItemStack base, String... extra) {
        return from(base).lore(extra).build();
    }

    public static String pdcString(ItemStack i, NamespacedKey k) {
        if (Util.isAir(i) || !i.hasItemMeta()) return null;
        return i.getItemMeta().getPersistentDataContainer().get(k, PersistentDataType.STRING);
    }

    public static int pdcInt(ItemStack i, NamespacedKey k, int def) {
        if (Util.isAir(i) || !i.hasItemMeta()) return def;
        Integer v = i.getItemMeta().getPersistentDataContainer().get(k, PersistentDataType.INTEGER);
        return v == null ? def : v;
    }
}
