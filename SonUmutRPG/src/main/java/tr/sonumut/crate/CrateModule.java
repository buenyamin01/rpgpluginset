package tr.sonumut.crate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.core.event.CrateOpenEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Kutular: crates.yml. Anahtarla bağlı bloğa sağ tıklayınca açılır; ödül sunucuda anında belirlenir, animasyon yalnızca gösterimdir. */
public final class CrateModule extends BaseModule implements Listener {
    static final class Reward {
        ItemStack item;     // null ise altın ödülü
        long gold;
        double weight = 10;
    }

    static final class Crate {
        final String id;
        String name;
        final List<Reward> rewards = new ArrayList<>();
        final List<String> blocks = new ArrayList<>();
        Crate(String id) { this.id = id; this.name = "<gold>" + id; }
    }

    final Map<String, Crate> crates = new TreeMap<>();
    private YamlStore store;

    public CrateModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "crate"; }

    @Override public void enable() {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "crates.yml"));
        load();
        listen(this);
        plugin.giveables().put("anahtar", extra -> extra.length == 0 || !crates.containsKey(extra[0].toLowerCase(Locale.ROOT)) ? null : keyItem(crates.get(extra[0].toLowerCase(Locale.ROOT))));
        plugin.registerAdmin("kutu", "crate", this::admin);
    }

    @Override public void disable() {
        cleanup();
        if (store != null) { save(); store.flush(); }
        plugin.giveables().remove("anahtar");
        plugin.unregisterAdmin("kutu");
    }

    @Override public void reload() { store.load(); load(); }

    private void admin(Player p, String[] a) {
        if (a.length == 0) { new CrateAdminMenu(this).open(p); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        Crate c = a.length > 1 ? crates.get(a[1].toLowerCase(Locale.ROOT)) : null;
        if (c == null) { Lang.send(p, "kutu.yok", "<red>Kutu bulunamadı. <gray>Kullanım: <white>/sonumut kutu blok|sil <id>"); return; }
        if (sub.equals("blok")) {
            Block b = p.getTargetBlockExact(6);
            if (b == null) { Lang.send(p, "kutu.blok-yok", "<red>Bir bloğa bak."); return; }
            String key = Util.locStr(b.getLocation());
            if (!c.blocks.contains(key)) c.blocks.add(key); else c.blocks.remove(key);
            save();
            Lang.send(p, "kutu.blok-tamam", "<green>Blok güncellendi (<durum>).", "durum", c.blocks.contains(key) ? "bağlandı" : "kaldırıldı");
        } else if (sub.equals("sil")) {
            crates.remove(c.id);
            save();
            Lang.send(p, "kutu.silindi", "<green>Kutu silindi.");
        }
    }

    // ---------- veri ----------

    private void load() {
        crates.clear();
        ConfigurationSection root = store.yaml().getConfigurationSection("crates");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Crate c = new Crate(id);
            c.name = s.getString("name", c.name);
            c.blocks.addAll(s.getStringList("blocks"));
            ConfigurationSection rs = s.getConfigurationSection("rewards");
            if (rs != null) {
                for (String k : rs.getKeys(false)) {
                    ConfigurationSection r = rs.getConfigurationSection(k);
                    if (r == null) continue;
                    Reward rw = new Reward();
                    rw.item = r.getItemStack("item");
                    rw.gold = r.getLong("gold", 0);
                    rw.weight = Math.max(0.0001, r.getDouble("weight", 10));
                    if (rw.item == null && rw.gold <= 0) continue;
                    c.rewards.add(rw);
                }
            }
            crates.put(id.toLowerCase(Locale.ROOT), c);
        }
    }

    void save() {
        YamlConfiguration y = store.yaml();
        y.set("crates", null);
        for (Crate c : crates.values()) {
            String b = "crates." + c.id;
            y.set(b + ".name", c.name);
            y.set(b + ".blocks", c.blocks);
            int i = 0;
            for (Reward r : c.rewards) {
                y.set(b + ".rewards." + i + ".item", r.item);
                y.set(b + ".rewards." + i + ".gold", r.gold);
                y.set(b + ".rewards." + i + ".weight", r.weight);
                i++;
            }
        }
        store.dirty();
    }

    // ---------- eşyalar / ödül ----------

    ItemStack keyItem(Crate c) {
        return ItemBuilder.of(Material.TRIPWIRE_HOOK).name(Lang.mm(c.name + " <gray>Anahtarı"))
                .lore("<gray>Bağlı kutuya sağ tıklayarak kullan.").glow(true).hideAll().str(Keys.CRATE_KEY, c.id).build();
    }

    static ItemStack rewardIcon(Reward r) {
        if (r.item != null) return r.item.clone();
        return ItemBuilder.of(Material.GOLD_INGOT).name(Lang.mm("<gold>" + Util.num(r.gold) + " Altın")).build();
    }

    static double total(Crate c) { double t = 0; for (Reward r : c.rewards) t += r.weight; return t; }

    static Rarity rarityOf(Crate c, Reward r) { return Rarity.fromChance(r.weight / Math.max(0.0001, total(c)) * 100); }

    Reward roll(Crate c) {
        double t = total(c), x = ThreadLocalRandom.current().nextDouble() * t, acc = 0;
        for (Reward r : c.rewards) { acc += r.weight; if (x < acc) return r; }
        return c.rewards.get(c.rewards.size() - 1);
    }

    private Crate crateAt(Block b) {
        String key = Util.locStr(b.getLocation());
        for (Crate c : crates.values()) if (c.blocks.contains(key)) return c;
        return null;
    }

    void open(Player p, Crate c) {
        if (c.rewards.isEmpty()) { Lang.send(p, "kutu.bos", "<red>Bu kutunun ödülü yok."); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!c.id.equals(ItemBuilder.pdcString(hand, Keys.CRATE_KEY))) {
            Fx.fail(p);
            Lang.send(p, "kutu.anahtar-yok", "<red>Bu kutu için elinde anahtar yok.");
            new CratePreviewMenu(this, c).open(p);
            return;
        }
        hand.setAmount(hand.getAmount() - 1);                       // önce anahtarı al
        p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        Reward r = roll(c);                                          // sonuç şimdi belirlenir
        EconomyService eco = Services.get(EconomyService.class);
        if (r.item != null) Util.give(p, r.item); else if (eco != null) eco.give(p.getUniqueId(), r.gold);
        Rarity rar = rarityOf(c, r);
        Bukkit.getPluginManager().callEvent(new CrateOpenEvent(p, c.id, r.item, r.gold));
        Io.log("crate", p.getName() + " " + c.id + " -> " + (r.item != null ? r.item.getType() + " x" + r.item.getAmount() : r.gold + " altın"));
        Rarity min = Rarity.parse(cfg().getString("crate.broadcast-from", "ESSIZ"), Rarity.ESSIZ);
        if (rar.ordinal() >= min.ordinal()) {
            String what = r.item != null ? (r.item.hasItemMeta() && r.item.getItemMeta().hasDisplayName() ? Lang.plain(r.item.getItemMeta().displayName()) : Util.pretty(r.item.getType().name())) : Util.num(r.gold) + " Altın";
            Bukkit.broadcast(Lang.t("kutu.duyuru", "<gold>★ <yellow><oyuncu> <gray>kutudan <white><esya> <gray>kazandı! <dark_gray>(<nadirlik>)", "oyuncu", p.getName(), "esya", what, "nadirlik", rar.ad));
        }
        new CrateMenu(this, c, r).open(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Crate c = crateAt(e.getClickedBlock());
        if (c == null) return;
        e.setCancelled(true);
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) new CratePreviewMenu(this, c).open(e.getPlayer());
        else open(e.getPlayer(), c);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (crateAt(e.getBlock()) != null) { e.setCancelled(true); Lang.send(e.getPlayer(), "kutu.kirilamaz", "<red>Bu blok bir kutuya bağlı."); }
    }
}
