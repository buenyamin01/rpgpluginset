package tr.sonumut.core;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import tr.sonumut.SonUmutPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Oyuncudan sohbet üzerinden tek satırlık girdi alır. "iptal" yazılırsa vazgeçilir. */
public final class PromptService implements Listener {
    private record Prompt(Consumer<String> ok, Runnable cancel, BukkitTask expiry) {}

    private final SonUmutPlugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public PromptService(SonUmutPlugin plugin) { this.plugin = plugin; }

    /** Soruyu gösterir; cevap ana thread'de ok'a, vazgeçme/zaman aşımı cancel'a gider (cancel null olabilir). */
    public void ask(Player p, Component question, Consumer<String> ok, Runnable cancel) {
        Prompt old = prompts.remove(p.getUniqueId());
        if (old != null) { old.expiry.cancel(); if (old.cancel != null) old.cancel.run(); }
        p.closeInventory();
        p.sendMessage(Lang.prefix().append(question));
        p.sendMessage(Lang.prefix().append(Lang.t("genel.iptal-ipucu", "<gray>Vazgeçmek için <yellow>iptal</yellow> yaz. (30 sn)")));
        UUID id = p.getUniqueId();
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Prompt pr = prompts.remove(id);
            if (pr == null) return;
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) Lang.send(pl, "genel.sure-doldu", "<red>Süre doldu.");
            if (pr.cancel != null) pr.cancel.run();
        }, 20L * 30);
        prompts.put(id, new Prompt(ok, cancel, expiry));
    }

    public void ask(Player p, String mmQuestion, Consumer<String> ok, Runnable cancel) {
        ask(p, Lang.mm(mmQuestion), ok, cancel);
    }

    public boolean has(Player p) { return prompts.containsKey(p.getUniqueId()); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Prompt pr = prompts.remove(id);
        if (pr == null) return;
        e.setCancelled(true);
        String text = Lang.plain(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            pr.expiry.cancel();
            if (text.equalsIgnoreCase("iptal")) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) Lang.send(p, "genel.iptal-edildi", "<gray>İşlem iptal edildi.");
                if (pr.cancel != null) pr.cancel.run();
            } else pr.ok.accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Prompt pr = prompts.remove(e.getPlayer().getUniqueId());
        if (pr != null) pr.expiry.cancel();
    }
}
