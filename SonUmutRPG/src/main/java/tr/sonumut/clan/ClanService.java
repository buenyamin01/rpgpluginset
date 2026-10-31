package tr.sonumut.clan;

import java.util.Optional;
import java.util.UUID;

public interface ClanService {
    Optional<Clan> of(UUID id);

    /** Klan etiketi; klanı yoksa boş metin. Async okumalar için güvenlidir. */
    String tag(UUID id);
}
