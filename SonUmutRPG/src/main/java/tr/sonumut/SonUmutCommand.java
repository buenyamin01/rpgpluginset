package tr.sonumut;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tr.sonumut.core.Giveable;
import tr.sonumut.core.Lang;
import tr.sonumut.core.Util;

import java.util.*;

/** /sonumut: reload, modul, debug, ver ve modüllerin yönetici komutları. */
public final class SonUmutCommand implements CommandExecutor, TabCompleter {
    private final SonUmutPlugin plugin;

    public SonUmutCommand(SonUmutPlugin plugin) { this.plugin = plugin; }

    private boolean isAdmin(CommandSender s) { return s.hasPermission("sonumut.admin"); }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("yardim")) { help(s); return true; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!isAdmin(s)) { noPerm(s); return true; }
                plugin.reloadConfig();
                Lang.reload();
                plugin.modules().reloadAll();
                Lang.send(s, "admin.reload", "<green>Ayarlar ve mesajlar yeniden yüklendi.");
            }
            case "modul" -> {
                if (!isAdmin(s)) { noPerm(s); return true; }
                if (a.length < 3) {
                    for (String id : plugin.modules().ids()) {
                        s.sendMessage(Lang.mm((plugin.modules().isActive(id) ? "<green>● " : "<red>● ") + id));
                    }
                    Lang.send(s, "admin.modul-kullanim", "<gray>Kullanım: <white>/sonumut modul <id> ac|kapat");
                    return true;
                }
                String id = a[1].toLowerCase(Locale.ROOT);
                if (!plugin.modules().ids().contains(id)) { Lang.send(s, "admin.modul-yok", "<red>Böyle bir modül yok."); return true; }
                boolean on = a[2].equalsIgnoreCase("ac") || a[2].equalsIgnoreCase("aç");
                plugin.getConfig().set("modules." + id, on);
                plugin.saveConfig();
                boolean ok = on ? plugin.modules().enable(id) : plugin.modules().disable(id);
                Lang.send(s, ok ? "admin.modul-tamam" : "admin.modul-degismedi",
                        ok ? "<green>Modül güncellendi: <white><id>" : "<yellow>Modül zaten bu durumda ya da açılamadı: <white><id>", "id", id);
            }
            case "debug" -> {
                if (!isAdmin(s)) { noPerm(s); return true; }
                boolean d = plugin.toggleDebug();
                Lang.send(s, "admin.debug", "<gray>Hata ayıklama: <white><durum>", "durum", d ? "açık" : "kapalı");
            }
            case "ver" -> give(s, a);
            default -> {
                SonUmutPlugin.AdminEntry e = plugin.admins().get(sub);
                if (e == null) { help(s); return true; }
                if (!isAdmin(s) && !s.hasPermission("sonumut." + e.module() + ".admin")) { noPerm(s); return true; }
                if (!(s instanceof Player p)) { Lang.send(s, "genel.sadece-oyuncu", "<red>Bu komut yalnızca oyunculara açık."); return true; }
                e.handler().run(p, Arrays.copyOfRange(a, 1, a.length));
            }
        }
        return true;
    }

    private void give(CommandSender s, String[] a) {
        if (!isAdmin(s)) { noPerm(s); return; }
        if (a.length < 3) {
            Lang.send(s, "admin.ver-kullanim", "<gray>Kullanım: <white>/sonumut ver <tür> <oyuncu> [adet] [ekstra]  <gray>Türler: <white><turler>",
                    "turler", String.join(", ", plugin.giveables().keySet()));
            return;
        }
        Giveable g = plugin.giveables().get(a[1].toLowerCase(Locale.ROOT));
        Player target = Bukkit.getPlayerExact(a[2]);
        if (g == null || target == null) { Lang.send(s, "admin.ver-yok", "<red>Tür veya oyuncu bulunamadı."); return; }
        int amount = a.length > 3 ? Math.max(1, Math.min(64, Util.parseInt(a[3], 1))) : 1;
        String[] extra = a.length > 4 ? Arrays.copyOfRange(a, 4, a.length) : new String[0];
        ItemStack it = g.make(extra);
        if (it == null) { Lang.send(s, "admin.ver-gecersiz", "<red>Geçersiz ekstra argüman."); return; }
        it.setAmount(amount);
        Util.give(target, it);
        Lang.send(s, "admin.ver-tamam", "<green><oyuncu> oyuncusuna verildi.", "oyuncu", target.getName());
    }

    private void noPerm(CommandSender s) { Lang.send(s, "genel.yetki-yok", "<red>Bunun için yetkin yok."); }

    private void help(CommandSender s) {
        Lang.send(s, "admin.yardim", "<gray>Komutlar: <white>reload, modul, debug, ver<gray> ve modül komutları: <white><anahtarlar>",
                "anahtarlar", String.join(", ", plugin.admins().keySet()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (!isAdmin(s) && plugin.admins().values().stream().noneMatch(e -> s.hasPermission("sonumut." + e.module() + ".admin"))) return out;
        if (a.length == 1) {
            out.addAll(List.of("reload", "modul", "debug", "ver"));
            out.addAll(plugin.admins().keySet());
        } else if (a.length == 2 && a[0].equalsIgnoreCase("modul")) out.addAll(plugin.modules().ids());
        else if (a.length == 3 && a[0].equalsIgnoreCase("modul")) out.addAll(List.of("ac", "kapat"));
        else if (a.length == 2 && a[0].equalsIgnoreCase("ver")) out.addAll(plugin.giveables().keySet());
        else if (a.length == 3 && a[0].equalsIgnoreCase("ver")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
