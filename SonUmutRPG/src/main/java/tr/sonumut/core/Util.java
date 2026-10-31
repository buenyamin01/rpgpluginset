package tr.sonumut.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public final class Util {
    private static final NumberFormat NF = NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"));

    private Util() {}

    public static synchronized String num(long n) { return NF.format(n); }

    public static boolean isAir(ItemStack i) { return i == null || i.getType() == Material.AIR || i.getAmount() <= 0; }

    /** Envantere ekler, sığmayanı oyuncunun ayağına bırakır. */
    public static void give(Player p, ItemStack item) {
        if (isAir(item)) return;
        Map<Integer, ItemStack> left = p.getInventory().addItem(item.clone());
        for (ItemStack i : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), i);
    }

    public static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    public static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim().replace(".", "").replace(",", "")); } catch (Exception e) { return def; }
    }

    public static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return def; }
    }

    /** ZOMBIE_VILLAGER -> Zombie Villager */
    public static String pretty(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** MiniMessage ilerleme çubuğu. */
    public static String bar(double frac, int len) {
        int f = (int) Math.round(clamp(frac, 0, 1) * len);
        return "<green>" + "█".repeat(f) + "<dark_gray>" + "█".repeat(len - f);
    }

    public static String locStr(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    public static Location parseLoc(String s) {
        if (s == null) return null;
        String[] a = s.split(";");
        if (a.length < 4) return null;
        World w = Bukkit.getWorld(a[0]);
        if (w == null) return null;
        try { return new Location(w, Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])); }
        catch (NumberFormatException e) { return null; }
    }
}
