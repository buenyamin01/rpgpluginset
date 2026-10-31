package tr.sonumut.trade;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.core.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** İki oyunculu takas oturumu. Eşyalar menü envanterlerinde durur; tamamlanırken sırayla aktarılır. */
final class TradeSession {
    final TradeModule mod;
    final Player a, b;
    final TradeMenu ma, mb;
    long goldA, goldB;
    boolean readyA, readyB, done;
    private int version;

    TradeSession(TradeModule mod, Player a, Player b) {
        this.mod = mod;
        this.a = a;
        this.b = b;
        this.ma = new TradeMenu(this, true);
        this.mb = new TradeMenu(this, false);
    }

    void open() {
        ma.open(a);
        mb.open(b);
        Lang.send(a, "takas.basladi", "<green>Takas başladı. Altın için: <white>/takas altin <miktar>");
        Lang.send(b, "takas.basladi", "<green>Takas başladı. Altın için: <white>/takas altin <miktar>");
    }

    TradeMenu menuOf(Player p) { return p.equals(a) ? ma : mb; }

    void changed() {
        version++;
        readyA = false;
        readyB = false;
        ma.refresh();
        mb.refresh();
    }

    void setGold(Player p, long amount) {
        if (done) return;
        EconomyService eco = Services.get(EconomyService.class);
        if (eco == null || !eco.has(p.getUniqueId(), amount)) { Fx.fail(p); Lang.send(p, "takas.altin-yok", "<red>Yeterli Altının yok."); return; }
        if (p.equals(a)) goldA = amount; else goldB = amount;
        Fx.click(p);
        changed();
    }

    void toggleReady(Player p) {
        if (done) return;
        if (p.equals(a)) readyA = !readyA; else readyB = !readyB;
        Fx.click(p);
        ma.refresh();
        mb.refresh();
        if (readyA && readyB) {
            int v = version;
            Lang.send(a, "takas.geri-sayim", "<yellow>İki taraf da hazır. 3 saniye içinde tamamlanıyor...");
            Lang.send(b, "takas.geri-sayim", "<yellow>İki taraf da hazır. 3 saniye içinde tamamlanıyor...");
            Bukkit.getScheduler().runTaskLater(mod.plugin(), () -> { if (!done && v == version && readyA && readyB) complete(); }, 60L);
        }
    }

    private void complete() {
        EconomyService eco = Services.get(EconomyService.class);
        if (eco == null || !a.isOnline() || !b.isOnline()) { cancel(null); return; }
        if (!eco.has(a.getUniqueId(), goldA) || !eco.has(b.getUniqueId(), goldB)) {
            Lang.send(a, "takas.altin-degisti", "<red>Altın yetersiz, takas iptal edildi.");
            Lang.send(b, "takas.altin-degisti", "<red>Altın yetersiz, takas iptal edildi.");
            cancel(null);
            return;
        }
        // 1) önce altınları al
        if (!eco.take(a.getUniqueId(), goldA)) { cancel(null); return; }
        if (!eco.take(b.getUniqueId(), goldB)) { eco.give(a.getUniqueId(), goldA); cancel(null); return; }
        // 2) eşyaları menülerden çıkar
        Map<Integer, ItemStack> fromA = new LinkedHashMap<>(ma.freeItems());
        Map<Integer, ItemStack> fromB = new LinkedHashMap<>(mb.freeItems());
        for (int s : fromA.keySet()) ma.clearSlot(s);
        for (int s : fromB.keySet()) mb.clearSlot(s);
        done = true;
        mod.sessions.remove(a.getUniqueId());
        mod.sessions.remove(b.getUniqueId());
        // 3) sonra ver
        eco.give(b.getUniqueId(), goldA);
        eco.give(a.getUniqueId(), goldB);
        for (ItemStack it : fromA.values()) Util.give(b, it);
        for (ItemStack it : fromB.values()) Util.give(a, it);
        Io.log("trade", a.getName() + " (" + goldA + " altın, " + fromA.size() + " eşya) <-> " + b.getName() + " (" + goldB + " altın, " + fromB.size() + " eşya)");
        a.closeInventory();
        b.closeInventory();
        Fx.levelUp(a);
        Fx.levelUp(b);
        Lang.send(a, "takas.tamam", "<green>Takas tamamlandı!");
        Lang.send(b, "takas.tamam", "<green>Takas tamamlandı!");
    }

    /** İptal; eşyalar menü kapanırken sahiplerine otomatik iade edilir. who null olabilir. */
    void cancel(Player who) {
        if (done) return;
        done = true;
        mod.sessions.remove(a.getUniqueId());
        mod.sessions.remove(b.getUniqueId());
        for (Player p : new Player[]{a, b}) {
            if (!p.isOnline()) continue;
            Lang.send(p, "takas.iptal", "<red>Takas iptal edildi.");
            if (!p.equals(who)) p.closeInventory();
        }
    }
}
