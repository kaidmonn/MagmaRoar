package me.yourname.ultimateitems.teams;
import org.bukkit.entity.Player;
import java.util.*;

public class Team {
    private UUID leader;
    private final int id;
    private final Set<UUID> members = new HashSet<>();

    public Team(Player leader, int id) {
        this.leader = leader.getUniqueId();
        this.id = id;
        this.members.add(leader.getUniqueId());
    }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public int getId() { return id; }
    public Set<UUID> getMembers() { return members; }
}