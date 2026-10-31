package tr.sonumut.hud;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tr.sonumut.SonUmutPlugin;
import tr.sonumut.clan.ClanService;
import tr.sonumut.core.*;
import tr.sonumut.party.PartyService;
import tr.sonumut.quest.QuestService;
import tr.sonumut.quest.QuestView;
import tr.sonumut.rpg.ClassType;
import tr.sonumut.rpg.PlayerProfile;
import tr.sonumut.rpg.ProfileService;

import java.util.*;

/** Kenar çubuğu (scoreboard), tab listesi, animasyonlu başlık ve actionbar HUD'u. */
public final class HudModule extends BaseModule implements HudService, Listener {
    private static final int MAX_LINES = 12;

    private static final class Board {
        final Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        final Objective obj;
        final Team[] teams = new Team[MAX_LINES];
        final String[] entries = new String[MAX_LINES];
        final String[] cache = new String[MAX_LINES];
        int size;

        Board() {
            obj = sb.registerNewObjective("rpg", Criteria.DUMMY, Component.text("SonUmut"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            blankNumbers(obj);
            for (int i = 0; i < MAX_LINES; i++) {
                entries[i] = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
                teams[i] = sb.registerNewTeam("l" + i);
                teams[i].addEntry(entries[i]);
            }
        }
    }

    /** Sağdaki kırmızı sayıları gizler. (Paper 1.20.4+ API; derleme hatası verirse bu satırı silmek yeterli.) */
    private static void blankNumbers(Objective o) { o.numberFormat(NumberFormat.blank()); }

    private final Map<UUID, Board> boards = new HashMap<>();
    private final Map<UUID, Long> suppress = new HashMap<>();
    private int phase;

    public HudModule(SonUmutPlugin plugin) { super(plugin); }

    @Override public String id() { return "hud"; }

    @Override public void enable() {
        Services.register(HudService.class, this);
        listen(this);
        every(10L, 10L, this::tick);
    }

    @Override public void disable() {
        cleanup();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            p.playerListName(null);
            p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
        boards.clear();
        suppress.clear();
        Services.unregister(HudService.class);
    }

    @Override public void flash(Player p, Component c) {
        suppress.put(p.getUniqueId(), System.currentTimeMillis() + 3000);
        p.sendActionBar(c);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
        suppress.remove(e.getPlayer().getUniqueId());
    }

    private void tick() {
        phase = (phase + 1) % 40;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { update(p); } catch (Throwable t) { plugin.debug("HUD hatası: " + t); }
        }
    }

    private void update(Player p) {
        ProfileService ps = Services.get(ProfileService.class);
        if (ps == null) return;
        PlayerProfile pr = ps.get(p);
        ClassType cls = pr.cls();
        ClanService clans = Services.get(ClanService.class);
        String clanTag = clans != null ? clans.tag(p.getUniqueId()) : "";

        if (cfg().getBoolean("hud.sidebar", true)) {
            Board b = boards.computeIfAbsent(p.getUniqueId(), k -> new Board());
            if (p.getScoreboard() != b.sb) p.setScoreboard(b.sb);
            double ph = phase / 20.0 - 1.0;
            b.obj.displayName(Lang.mm(String.format(Locale.ROOT, "<gradient:#ffd27f:#ff7e5f:%.2f><bold>SonUmut RPG</bold></gradient>", ph)));
            List<String> lines = new ArrayList<>();
            EconomyService eco = Services.get(EconomyService.class);
            long need = ps.xpNeeded(pr.level());
            lines.add("<dark_gray><strikethrough>              ");
            lines.add("<gray>Sınıf: " + (cls == null ? "<red>Seçilmedi" : cls.gradient()));
            lines.add("<gray>Seviye: <white>" + pr.level() + " <dark_gray>(" + (int) (100.0 * pr.xp() / need) + "%)");
            lines.add(Util.bar((double) pr.xp() / need, 14));
            if (eco != null) lines.add("<gray>Altın: <gold>" + Util.num(eco.gold(p.getUniqueId())));
            if (!clanTag.isEmpty()) lines.add("<gray>Klan: <yellow>[" + clanTag + "]");
            PartyService party = Services.get(PartyService.class);
            if (party != null && party.of(p.getUniqueId()).isPresent()) lines.add("<gray>Parti: <aqua>" + party.of(p.getUniqueId()).get().members().size() + " kişi");
            QuestService quests = Services.get(QuestService.class);
            if (quests != null) {
                List<QuestView> tr = quests.tracked(p);
                if (!tr.isEmpty()) {
                    QuestView q = tr.get(0);
                    lines.add("<gray>Görev: <white>" + (q.name().length() > 14 ? q.name().substring(0, 14) : q.name()));
                    lines.add("<dark_gray> » <green>" + q.progress() + "<gray>/<green>" + q.goal());
                }
            }
            lines.add("<dark_gray><strikethrough>              ");
            while (lines.size() > MAX_LINES) lines.remove(lines.size() - 2);
            applyLines(b, lines);
        }

        if (cfg().getBoolean("hud.tab", true)) {
            String cp = cls == null ? "" : cls.ikon + " ";
            p.playerListName(Lang.mm("<dark_gray>[<yellow>" + pr.level() + "<dark_gray>] <white>" + cp + (clanTag.isEmpty() ? "" : "<gold>[" + clanTag + "] ") + "<white><n>", "n", p.getName()));
            p.sendPlayerListHeaderAndFooter(
                    Lang.mm(String.format(Locale.ROOT, "\n<gradient:#ffd27f:#ff7e5f:%.2f><bold>SonUmut RPG</bold></gradient>\n", phase / 20.0 - 1.0)),
                    Lang.mm("\n<gray>Çevrimiçi: <white>" + Bukkit.getOnlinePlayers().size() + " <dark_gray>| <gray>Ping: <white>" + p.getPing() + " ms\n"));
        }

        if (cfg().getBoolean("hud.actionbar", true)) {
            Long until = suppress.get(p.getUniqueId());
            if (until == null || until < System.currentTimeMillis()) {
                AttributeInstance h = p.getAttribute(Attribute.MAX_HEALTH);
                double max = h != null ? h.getValue() : 20;
                p.sendActionBar(Lang.mm("<red>❤ " + (int) Math.ceil(p.getHealth()) + "<gray>/<red>" + (int) max
                        + "    <aqua>✦ " + (int) ps.mana(p) + "<gray>/<aqua>" + (int) ps.maxMana(p)));
            }
        }
    }

    private void applyLines(Board b, List<String> lines) {
        for (int i = 0; i < MAX_LINES; i++) {
            if (i < lines.size()) {
                String l = lines.get(i);
                if (!l.equals(b.cache[i])) {
                    b.teams[i].prefix(Lang.mm(l));
                    b.cache[i] = l;
                }
            } else if (b.cache[i] != null) {
                b.sb.resetScores(b.entries[i]);
                b.cache[i] = null;
            }
        }
        if (b.size != lines.size()) {
            for (int i = 0; i < lines.size(); i++) b.obj.getScore(b.entries[i]).setScore(lines.size() - i);
            b.size = lines.size();
        }
    }
}
