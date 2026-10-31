package tr.sonumut.party;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PartyService {
    Optional<Party> of(UUID id);

    /** XP/ödül paylaşımına katılacak oyuncular: öldüren + yakındaki parti üyeleri. Partisi yoksa yalnızca öldüren. */
    Set<UUID> sharers(Player killer, Location loc);
}
