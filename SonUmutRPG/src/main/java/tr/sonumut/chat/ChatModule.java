package tr.sonumut.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.clan.ClanService;
import tr.sonumut.core.*;
import tr.sonumut.rpg.ClassType;
import tr.sonumut.rpg.PlayerProfile;
import tr.sonumut.rpg.ProfileService;

import java.util.Locale;

/** Sohbet biçimi: [Sv.N] sınıf [KLAN] İsim » mesaj, üzerine gelince kart, @isim ile etiketleme, & renk kodları (yetkiyle). */
public final class ChatModule extends BaseModule implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private volatile String format;

    public ChatModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "chat"; }

    @Override public void enable() {
        format = Lang.raw("sohbet.bicim", "<sv> <sinif><klan><isim><dark_gray> » <white><mesaj>");
        listen(this);
    }

    @Override public void disable() { cleanup(); }

    @Override public void reload() { format = Lang.raw("sohbet.bicim", "<sv> <sinif><klan><isim><dark_gray> » <white><mesaj>"); }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChatColor(AsyncChatEvent e) {
        if (e.getPlayer().hasPermission("sonumut.chat.renk")) {
            e.message(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.plain(e.message())));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        e.renderer(ChatRenderer.viewerUnaware((source, name, message) -> build(source, message)));
    }

    /** Async çalışır: yalnızca bellekteki profile ve klan haritalarını okur. */
    private Component build(Player p, Component message) {
        ProfileService ps = Services.get(ProfileService.class);
        ClanService clans = Services.get(ClanService.class);
        PlayerProfile pr = ps != null ? ps.peek(p.getUniqueId()) : null;
        int lvl = pr != null ? pr.level() : 1;
        ClassType cls = pr != null ? pr.cls() : null;
        String tag = clans != null ? clans.tag(p.getUniqueId()) : "";
        String lc = lvl < 10 ? "<green>" : lvl < 30 ? "<yellow>" : lvl < 60 ? "<gold>" : "<red>";

        Component sv = Lang.mm("<dark_gray>[" + lc + "Sv." + lvl + "<dark_gray>]");
        Component sinif = cls == null ? Component.empty() : Lang.mm(cls.gradient().replace(cls.ad, cls.ikon) + " ");
        Component klan = tag.isEmpty() ? Component.empty() : Lang.mm("<gold>[<tag>] ", "tag", tag);
        String nameMm = cls == null ? "<white><n>" : "<gradient:" + cls.renk1 + ":" + cls.renk2 + "><n></gradient>";
        Component hover = Lang.mm("<yellow><n>\n<gray>Seviye: <white>" + lvl + "\n<gray>Sınıf: " + (cls == null ? "<red>Yok" : cls.gradient())
                + (tag.isEmpty() ? "" : "\n<gray>Klan: <gold>[" + tag + "]") + "\n<dark_gray>Tıkla: özel mesaj", "n", p.getName());
        Component isim = Lang.mm(nameMm, "n", p.getName())
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.suggestCommand("/msg " + p.getName() + " "));
        return MM.deserialize(format, Placeholder.component("sv", sv), Placeholder.component("sinif", sinif),
                Placeholder.component("klan", klan), Placeholder.component("isim", isim), Placeholder.component("mesaj", message));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMention(AsyncChatEvent e) {
        String text = Lang.plain(e.message()).toLowerCase(Locale.ROOT);
        if (!text.contains("@")) return;
        String from = e.getPlayer().getName();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player t : Bukkit.getOnlinePlayers()) {
                if (t.getName().equals(from) || !text.contains("@" + t.getName().toLowerCase(Locale.ROOT))) continue;
                Fx.sound(t, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f);
                Fx.action(t, Lang.t("sohbet.etiket", "<yellow>@ <white><oyuncu> <gray>seni etiketledi", "oyuncu", from));
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(Lang.t("sohbet.giris", "<dark_gray>[<green>+<dark_gray>] <gray><oyuncu>", "oyuncu", e.getPlayer().getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(Lang.t("sohbet.cikis", "<dark_gray>[<red>-<dark_gray>] <gray><oyuncu>", "oyuncu", e.getPlayer().getName()));
    }
}
