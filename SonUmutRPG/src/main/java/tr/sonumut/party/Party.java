package tr.sonumut.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID leader() { return leader; }

    public Set<UUID> members() { return members; }

    void setLeader(UUID id) { leader = id; }
}
