package tr.sonumut.loot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface LootService {
    /** Mob tablosuna göre düşecek eşyaları belirler (ganimet şansı dahil). Sunucuda, tek seferde. */
    List<ItemStack> roll(LivingEntity mob, Player killer);
}
