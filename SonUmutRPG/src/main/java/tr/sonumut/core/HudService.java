package tr.sonumut.core;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface HudService {
    /** Actionbar mesajı gönderir ve HUD actionbar'ını kısa süre bastırır. */
    void flash(Player p, Component c);
}
