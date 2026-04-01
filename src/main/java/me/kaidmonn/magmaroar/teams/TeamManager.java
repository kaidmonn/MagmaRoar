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
    private final Map<UUID, Integer> pendingInvites = new HashMap<>(); // Кому пришел инвайт -> ID команды
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    public void createTeam(int id, Player owner) {
        if (teams.containsKey(id)) return;
        addPlayer(id, owner);
        setOwner(id, owner.getUniqueId());
    }

    public void addInvite(UUID player, int teamId) {
        pendingInvites.put(player, teamId);
    }

    public Integer getInvite(UUID player) {
        return pendingInvites.get(player);
    }

    public void removeInvite(UUID player) {
        pendingInvites.remove(player);
    }

    public void disband(int id) {
        List<UUID> members = teams.remove(id);
        if (members != null) {
            for (UUID uuid : members) {
                playerTeam.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.playerListName(null);
                    Team t = scoreboard.getEntryTeam(p.getName());
                    if (t != null) t.removeEntry(p.getName());
                }
            }
        }
        teamOwners.remove(id);
    }

    public void leaveTeam(Player p) {
        Integer id = playerTeam.remove(p.getUniqueId());
        if (id != null) {
            List<UUID> members = teams.get(id);
            if (members != null) {
                members.remove(p.getUniqueId());
                if (members.isEmpty()) disband(id);
            }
            p.playerListName(null);
            Team t = scoreboard.getEntryTeam(p.getName());
            if (t != null) t.removeEntry(p.getName());
        }
    }

    public void addPlayer(int id, Player player) {
        teams.computeIfAbsent(id, k -> new ArrayList<>());
        if (teams.get(id).size() >= 3) return;
        
        teams.get(id).add(player.getUniqueId());
        playerTeam.put(player.getUniqueId(), id);
        updateScoreboard(player, id);
    }

    private void updateScoreboard(Player p, int id) {
        String name = "team_" + id;
        Team t = scoreboard.getTeam(name);
        if (t == null) {
            t = scoreboard.registerNewTeam(name);
            List<NamedTextColor> colors = new ArrayList<>(NamedTextColor.NAMES.values());
            t.color(colors.get(Math.abs(id) % colors.size()));
            t.prefix(Component.text("[" + id + "] "));
        }
        t.addEntry(p.getName());
        p.playerListName(Component.text("[" + id + "] " + p.getName()));
    }

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

    public void setOwner(int id, UUID owner) { teamOwners.put(id, owner); }
    public UUID getOwner(int id) { return teamOwners.get(id); }
    public Integer getTeamId(UUID uuid) { return playerTeam.get(uuid); }
}