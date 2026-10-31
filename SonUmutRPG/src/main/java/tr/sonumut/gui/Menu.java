package tr.sonumut.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.ItemBuilder;
import tr.sonumut.core.Util;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * GUI çerçevesi. Varsayılan olarak her tıklama/sürükleme/shift/sayı tuşu iptal edilir.
 * Yalnızca freeSlot(...) ile tanımlanan "eşya yerleştirme" slotları serbesttir.
 * Menü kapanınca serbest slotlardaki eşyalar oyuncuya iade edilir.
 */
public abstract class Menu implements InventoryHolder {
    protected final Inventory inv;
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();
    private final Set<Integer> free = new LinkedHashSet<>();
    private final List<BukkitRunnable> tasks = new ArrayList<>();
    protected Player viewer;
    /** true iken tüm tıklamalar iptal edilir (animasyon/işlem sırasında). */
    public boolean locked;
    protected boolean closed;

    protected Menu(Component title, int rows) {
        this.inv = Bukkit.createInventory(this, rows * 9, title);
    }

    @NotNull @Override public Inventory getInventory() { return inv; }

    /** Alt sınıflar içeriği burada kurar. open() ve refresh() çağırır. */
    protected void render() {}

    public void open(Player p) {
        viewer = p;
        closed = false;
        render();
        p.openInventory(inv);
    }

    /** Serbest slotlar hariç her şeyi temizleyip yeniden çizer. */
    public void refresh() {
        for (int i = 0; i < inv.getSize(); i++) if (!free.contains(i)) inv.setItem(i, null);
        handlers.clear();
        render();
    }

    protected void set(int slot, ItemStack item, ClickHandler h) {
        if (slot < 0 || slot >= inv.getSize()) return;
        inv.setItem(slot, item);
        if (h != null) handlers.put(slot, h); else handlers.remove(slot);
    }

    protected void set(int slot, ItemStack item) { set(slot, item, null); }

    protected void freeSlot(int... slots) { for (int s : slots) free.add(s); }

    public Set<Integer> freeSlots() { return free; }

    public boolean isFree(int slot) { return free.contains(slot); }

    public ClickHandler handler(int slot) { return handlers.get(slot); }

    protected static ItemStack pane(Material m) { return ItemBuilder.of(m).name(" ").hideAll().build(); }

    /** Kenarları cam ile doldurur (boş ve serbest olmayan slotlar). */
    protected void border(Material m) {
        int rows = inv.getSize() / 9;
        for (int i = 0; i < inv.getSize(); i++) {
            int r = i / 9, c = i % 9;
            if ((r == 0 || r == rows - 1 || c == 0 || c == 8) && !free.contains(i) && inv.getItem(i) == null) inv.setItem(i, pane(m));
        }
    }

    protected void fill(Material m) {
        for (int i = 0; i < inv.getSize(); i++) if (!free.contains(i) && inv.getItem(i) == null) inv.setItem(i, pane(m));
    }

    /** Menü kapanınca otomatik iptal olan animasyon. frame: 0,1,2... */
    protected BukkitRunnable animate(long period, IntConsumer frame) {
        int[] n = {0};
        BukkitRunnable r = new BukkitRunnable() {
            @Override public void run() {
                if (closed || viewer == null || !viewer.isOnline()) { cancel(); return; }
                frame.accept(n[0]++);
            }
        };
        r.runTaskTimer(SonUmutPlugin.get(), 0L, Math.max(1L, period));
        tasks.add(r);
        return r;
    }

    /** Serbest slotlarda değişiklik oldu (1 tick sonra onFreeChange çağrılır). */
    public void freeChanged(Player p) {
        Bukkit.getScheduler().runTask(SonUmutPlugin.get(), () -> { if (!closed) onFreeChange(p); });
    }

    protected void onFreeChange(Player p) {}

    protected void onClosed(Player p) {}

    /** Serbest slotlardaki eşyalar; (slot -> eşya). */
    public Map<Integer, ItemStack> freeItems() {
        Map<Integer, ItemStack> m = new LinkedHashMap<>();
        for (int s : free) { ItemStack it = inv.getItem(s); if (!Util.isAir(it)) m.put(s, it); }
        return m;
    }

    public final void handleClose(Player p) {
        if (closed) return;
        closed = true;
        for (BukkitRunnable r : tasks) { try { r.cancel(); } catch (IllegalStateException ignored) { } }
        tasks.clear();
        for (int s : free) {
            ItemStack it = inv.getItem(s);
            if (!Util.isAir(it)) { inv.setItem(s, null); Util.give(p, it); }
        }
        onClosed(p);
    }
}
