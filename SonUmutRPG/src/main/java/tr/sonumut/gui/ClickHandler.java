package tr.sonumut.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface ClickHandler {
    void handle(Player p, InventoryClickEvent e);
}
