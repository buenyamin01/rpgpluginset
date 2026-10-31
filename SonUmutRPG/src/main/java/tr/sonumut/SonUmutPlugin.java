package tr.sonumut;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tr.sonumut.boss.BossModule;
import tr.sonumut.chat.ChatModule;
import tr.sonumut.clan.ClanModule;
import tr.sonumut.core.*;
import tr.sonumut.crate.CrateModule;
import tr.sonumut.forge.ForgeModule;
import tr.sonumut.gui.Menu;
import tr.sonumut.gui.MenuListener;
import tr.sonumut.hud.HudModule;
import tr.sonumut.loot.LootModule;
import tr.sonumut.megaphone.MegaphoneModule;
import tr.sonumut.mob.MobLevelModule;
import tr.sonumut.party.PartyModule;
import tr.sonumut.quest.QuestModule;
import tr.sonumut.rpg.ProfileModule;
import tr.sonumut.trade.TradeModule;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SonUmutPlugin extends JavaPlugin {
    public record AdminEntry(String module, AdminHandler handler) {}

    private static SonUmutPlugin instance;
    private ModuleManager modules;
    private EconomyImpl economy;
    private final Map<String, AdminEntry> admins = new LinkedHashMap<>();
    private final Map<String, Giveable> giveables = new LinkedHashMap<>();
    private boolean debug;

    private final CommandExecutor disabledExecutor = (sender, cmd, label, args) -> {
        Lang.send(sender, "genel.modul-kapali", "<red>Bu özellik şu anda kapalı.");
        return true;
    };

    public static SonUmutPlugin get() { return instance; }

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Io.start(getDataFolder());
        Lang.init(this);
        Keys.init(this);
        economy = new EconomyImpl(this);
        Services.register(EconomyService.class, economy);
        PromptService prompts = new PromptService(this);
        Services.register(PromptService.class, prompts);
        getServer().getPluginManager().registerEvents(prompts, this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        modules = new ModuleManager(this);
        modules.add(new ProfileModule(this));
        modules.add(new PartyModule(this));
        modules.add(new ClanModule(this));
        modules.add(new MobLevelModule(this));
        modules.add(new LootModule(this));
        modules.add(new ForgeModule(this));
        modules.add(new CrateModule(this));
        modules.add(new QuestModule(this));
        modules.add(new BossModule(this));
        modules.add(new TradeModule(this));
        modules.add(new MegaphoneModule(this));
        modules.add(new HudModule(this));
        modules.add(new ChatModule(this));
        modules.enableAll();

        SonUmutCommand cmd = new SonUmutCommand(this);
        PluginCommand pc = getCommand("sonumut");
        if (pc != null) { pc.setExecutor(cmd); pc.setTabCompleter(cmd); }
        getLogger().info("SonUmutRPG hazır.");
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        }
        if (modules != null) modules.disableAll();
        if (economy != null) economy.flush();
        Lang.flush();
        Services.clear();
        Io.shutdown();
        instance = null;
    }

    public ModuleManager modules() { return modules; }

    public Map<String, AdminEntry> admins() { return admins; }

    public Map<String, Giveable> giveables() { return giveables; }

    public void registerAdmin(String key, String module, AdminHandler h) { admins.put(key, new AdminEntry(module, h)); }

    public void unregisterAdmin(String key) { admins.remove(key); }

    public boolean debug() { return debug; }

    public boolean toggleDebug() { debug = !debug; return debug; }

    public void debug(String msg) { if (debug) getLogger().info("[debug] " + msg); }

    /** plugin.yml'deki komuta modül yürütücüsünü bağlar. */
    public void bind(String name, CommandExecutor ex) {
        PluginCommand c = getCommand(name);
        if (c == null) return;
        c.setExecutor(ex);
        c.setTabCompleter(ex instanceof TabCompleter t ? t : null);
    }

    /** Modül kapanınca komutu "kapalı" yanıtına çevirir. */
    public void unbind(String... names) {
        for (String n : names) {
            PluginCommand c = getCommand(n);
            if (c != null) { c.setExecutor(disabledExecutor); c.setTabCompleter(null); }
        }
    }
}
