package tr.sonumut.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.Util;

public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu m)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (m.locked) { e.setCancelled(true); return; }
        ClickType c = e.getClick();
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR || c == ClickType.DOUBLE_CLICK) { e.setCancelled(true); return; }
        Inventory clicked = e.getClickedInventory();
        if (clicked == null) { e.setCancelled(true); return; }
        if (clicked.equals(top)) {
            int s = e.getRawSlot();
            if (m.isFree(s)) {
                if (c == ClickType.NUMBER_KEY || c == ClickType.SWAP_OFFHAND) { e.setCancelled(true); return; }
                e.setCancelled(false);
                m.freeChanged(p);
                return;
            }
            e.setCancelled(true);
            ClickHandler h = m.handler(s);
            if (h != null) {
                try { h.handle(p, e); }
                catch (Throwable t) { Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "[SonUmutRPG] Menü tıklama hatası", t); }
            }
        } else if (c.isShiftClick()) {
            // Alt envanterden shift-tık: yalnızca serbest slotlara elle taşı.
            e.setCancelled(true);
            ItemStack cur = e.getCurrentItem();
            if (Util.isAir(cur)) return;
            for (int s : m.freeSlots()) {
                if (Util.isAir(top.getItem(s))) {
                    top.setItem(s, cur.clone());
                    e.setCurrentItem(null);
                    m.freeChanged(p);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu m)) return;
        if (m.locked) { e.setCancelled(true); return; }
        boolean touchesTop = false;
        for (int raw : e.getRawSlots()) {
            if (raw < top.getSize()) {
                touchesTop = true;
                if (!m.isFree(raw)) { e.setCancelled(true); return; }
            }
        }
        if (touchesTop && e.getWhoClicked() instanceof Player p) m.freeChanged(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu m && e.getPlayer() instanceof Player p) m.handleClose(p);
    }
}
