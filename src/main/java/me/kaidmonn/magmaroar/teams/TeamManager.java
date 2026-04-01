package me.kaidmonn.magmaroar.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
        for (UUID uuid : teams.getOrDefault(id, new ArrayList<>())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) members.add(p);
        }
        return members;
    }

    public void addPlayer(int id, Player player) {
        if (!teams.containsKey(id)) {
            teams.put(id, new ArrayList<>());
        }
        
        if (teams.get(id).size() >= 3) return; // Лимит 3 человека
        
        teams.get(id).add(player.getUniqueId());
        playerTeam.put(player.getUniqueId(), id);
        updateScoreboard(player, id);
    }

    private void updateScoreboard(Player p, int id) {
        String name = "team_" + id;
        Team t = scoreboard.getTeam(name);
        if (t == null) {
            t = scoreboard.registerNewTeam(name);
            t.color(NamedTextColor.values()[id % 15]);
            t.prefix(Component.text("[" + id + "] "));
        }
        t.addEntry(p.getName());
        p.playerListName(Component.text("[" + id + "] " + p.getName()));
    }

    public void setOwner(int id, UUID owner) { teamOwners.put(id, owner); }
    public UUID getOwner(int id) { return teamOwners.get(id); }
    public Integer getTeamId(UUID uuid) { return playerTeam.get(uuid); }
}