package tr.sonumut.core;

import org.bukkit.entity.Player;

/** /sonumut <anahtar> ... altındaki modül yönetici komutları. args: anahtardan sonraki argümanlar. */
@FunctionalInterface
public interface AdminHandler {
    void run(Player p, String[] args);
}
