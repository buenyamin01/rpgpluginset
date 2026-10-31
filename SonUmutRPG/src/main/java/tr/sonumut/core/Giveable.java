package tr.sonumut.core;

import org.bukkit.inventory.ItemStack;

/** /sonumut ver <tür> ... ile verilebilen özel eşya üreticisi. extra: oyuncu ve adetten sonraki argümanlar. */
@FunctionalInterface
public interface Giveable {
    /** Eşyayı üretir; geçersiz argümanda null döner. */
    ItemStack make(String[] extra);
}
