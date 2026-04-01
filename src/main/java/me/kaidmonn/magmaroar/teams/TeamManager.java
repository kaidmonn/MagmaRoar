package me.kaidmonn.magmaroar.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import java.util.*;

public class TeamManager {
    private final Map<Integer, List<UUID>> teams = new HashMap<>();
    private final Map<UUID, Integer> playerTeam = new HashMap<>();
    private final Map<Integer, UUID> teamOwners = new HashMap<>();
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    public boolean isTeammate(Player p1, Player p2) {
        Integer t1 = playerTeam.get(p1.getUniqueId());
        Integer t2 = playerTeam.get(p2.getUniqueId());
        return t1 != null && t1.equals(t2);
    }

    public List<Player> getTeamMembers(Player player) {
        Integer id = playerTeam.get(player.getUniqueId());
        if (id == null) return Collections.singletonList(player);
        List<Player> members = new ArrayList<>();
        for (UUID uuid : teams.get(id)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) members.add(p);
        }
        return members;
    }

    public void createTeam(int id, Player owner) {
        if (playerTeam.containsKey(owner.getUniqueId())) return;
        teams.put(id, new ArrayList<>(List.of(owner.getUniqueId())));
        playerTeam.put(owner.getUniqueId(), id);
        teamOwners.put(id, owner.getUniqueId());
        updateScoreboard(owner, id);
    }

    public void addPlayer(int id, Player player) {
        if (teams.containsKey(id) && teams.get(id).size() < 3) {
            teams.get(id).add(player.getUniqueId());
            playerTeam.put(player.getUniqueId(), id);
            updateScoreboard(player, id);
        }
    }

    private void updateScoreboard(Player p, int id) {
        String name = "team_" + id;
        Team t = scoreboard.getTeam(name);
        if (t == null) {
            t = scoreboard.registerNewTeam(name);
            t.prefix(Component.text("[" + id + "] "));
        }
        t.addEntry(p.getName());
    }

    public Integer getTeamId(UUID uuid) { return playerTeam.get(uuid); }
    public UUID getOwner(int id) { return teamOwners.get(id); }
    public void leaveTeam(Player p) { /* Логика удаления */ }
    public void disband(int id) { /* Логика роспуска */ }
}