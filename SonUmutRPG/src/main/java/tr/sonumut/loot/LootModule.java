package tr.sonumut.loot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.core.*;
import tr.sonumut.mob.MobLevelService;
import tr.sonumut.rpg.CombatStat;
import tr.sonumut.rpg.ProfileService;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** mobloot.yml: mob türüne göre ganimet tabloları ("ALL" her mob için). Yönetici GUI: /sonumut loot */
public final class LootModule extends BaseModule implements LootService, Listener {
    static final class Entry {
        ItemStack item;
        double chance = 10;
        int min = 1, max = 1, minLevel = 1;
    }

    final Map<String, List<Entry>> tables = new TreeMap<>();
    private YamlStore store;

    public LootModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "loot"; }

    @Override public void enable() {
        store = new YamlStore(plugin, new File(plugin.getDataFolder(), "mobloot.yml"));
        boolean fresh = !store.exists();
        load();
        if (fresh) {
            addDefault("ZOMBIE", Material.IRON_INGOT, 8);
            addDefault("SKELETON", Material.GOLD_NUGGET, 12);
            save();
        }
        Services.register(LootService.class, this);
        listen(this);
        plugin.registerAdmin("loot", "loot", (p, a) -> new LootAdminMenu(this).open(p));
    }

    private void addDefault(String type, Material m, double chance) {
        Entry e = new Entry();
        e.item = new ItemStack(m);
        e.chance = chance;
        tables.computeIfAbsent(type, k -> new ArrayList<>()).add(e);
    }

    @Override public void disable() {
        cleanup();
        if (store != null) { save(); store.flush(); }
        Services.unregister(LootService.class);
        plugin.unregisterAdmin("loot");
    }

    @Override public void reload() { store.load(); load(); }

    private void load() {
        tables.clear();
        ConfigurationSection root = store.yaml().getConfigurationSection("tables");
        if (root == null) return;
        for (String type : root.getKeys(false)) {
            ConfigurationSection ts = root.getConfigurationSection(type);
            if (ts == null) continue;
            List<Entry> list = new ArrayList<>();
            for (String k : ts.getKeys(false)) {
                ConfigurationSection es = ts.getConfigurationSection(k);
                if (es == null) continue;
                ItemStack it = es.getItemStack("item");
                if (Util.isAir(it)) continue;
                Entry en = new Entry();
                en.item = it;
                en.chance = es.getDouble("chance", 10);
                en.min = Math.max(1, es.getInt("min", 1));
                en.max = Math.max(en.min, es.getInt("max", 1));
                en.minLevel = Math.max(1, es.getInt("minLevel", 1));
                list.add(en);
            }
            tables.put(type.toUpperCase(Locale.ROOT), list);
        }
    }

    /** Bellekteki tabloları YAML'a yazar (gecikmeli, asenkron). */
    void save() {
        YamlConfiguration y = store.yaml();
        y.set("tables", null);
        for (Map.Entry<String, List<Entry>> t : tables.entrySet()) {
            int i = 0;
            for (Entry e : t.getValue()) {
                String base = "tables." + t.getKey() + "." + (i++);
                y.set(base + ".item", e.item);
                y.set(base + ".chance", e.chance);
                y.set(base + ".min", e.min);
                y.set(base + ".max", e.max);
                y.set(base + ".minLevel", e.minLevel);
            }
        }
        store.dirty();
    }

    @Override public List<ItemStack> roll(LivingEntity mob, Player killer) {
        List<ItemStack> out = new ArrayList<>();
        MobLevelService ml = Services.get(MobLevelService.class);
        ProfileService ps = Services.get(ProfileService.class);
        int level = ml != null ? ml.levelOf(mob) : 1;
        double bonus = ps != null && killer != null ? ps.combatStat(killer, CombatStat.GANIMET_SANSI) : 0;
        List<Entry> all = new ArrayList<>();
        all.addAll(tables.getOrDefault(mob.getType().name(), List.of()));
        all.addAll(tables.getOrDefault("ALL", List.of()));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Entry e : all) {
            if (level < e.minLevel) continue;
            if (r.nextDouble() * 100 >= e.chance * (1 + bonus / 100.0)) continue;
            ItemStack it = e.item.clone();
            it.setAmount(Math.min(it.getMaxStackSize(), e.min == e.max ? e.min : r.nextInt(e.min, e.max + 1)));
            out.add(it);
        }
        return out;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (m instanceof Player) return;
        var c = m.getPersistentDataContainer();
        if (c.has(Keys.BOSS_ID, PersistentDataType.STRING) || c.has(Keys.NO_REWARD, PersistentDataType.INTEGER)) return;
        Player killer = m.getKiller();
        if (killer == null) return;
        e.getDrops().addAll(roll(m, killer));
    }
}
