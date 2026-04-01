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

    // ПРОВЕРКА НА КОМАНДУ (Для обработчиков оружия)
    public boolean isTeammate(Player p1, Player p2) {
        Integer t1 = playerTeam.get(p1.getUniqueId());
        Integer t2 = playerTeam.get(p2.getUniqueId());
        return t1 != null && t1.equals(t2);
    }

    // ПОЛУЧЕНИЕ СПИСКА ИГРОКОВ (Для передачи эффектов косы)
    public List<Player> getTeamMembers(Player player) {
        Integer id = playerTeam.get(player.getUniqueId());
        if (id == null) return Collections.singletonList(player);
        
        List<Player> players = new ArrayList<>();
        for (UUID uuid : teams.get(id)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }

    public void createTeam(int id, Player owner) {
        if (playerTeam.containsKey(owner.getUniqueId())) return;
        teams.put(id, new ArrayList<>(Collections.singletonList(owner.getUniqueId())));
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

    public void leaveTeam(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerTeam.containsKey(uuid)) return;
        int id = playerTeam.get(uuid);
        teams.get(id).remove(uuid);
        playerTeam.remove(uuid);
        Team t = scoreboard.getTeam("team_" + id);
        if (t != null) t.removeEntry(player.getName());
        player.displayName(Component.text(player.getName()));
    }

    public void disband(int id) {
        if (!teams.containsKey(id)) return;
        for (UUID uuid : teams.get(id)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                playerTeam.remove(uuid);
                p.displayName(Component.text(p.getName()));
            }
        }
        teams.remove(id);
        teamOwners.remove(id);
        Team t = scoreboard.getTeam("team_" + id);
        if (t != null) t.unregister();
    }

    private void updateScoreboard(Player p, int id) {
        String teamName = "team_" + id;
        Team t = scoreboard.getTeam(teamName);
        if (t == null) {
            t = scoreboard.registerNewTeam(teamName);
            t.prefix(Component.text("[" + id + "] ").color(NamedTextColor.GOLD));
        }
        t.addEntry(p.getName());
    }

    public Integer getTeamId(UUID uuid) { return playerTeam.get(uuid); }
    public UUID getOwner(int id) { return teamOwners.get(id); }
}