package me.yourname.ultimateitems.teams;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import java.util.*;

public class TeamManager {
    private final Map<Integer, Team> teams = new HashMap<>();
    private final Map<UUID, Integer> playerToTeam = new HashMap<>();

    public Team getTeam(Player p) {
        Integer id = playerToTeam.get(p.getUniqueId());
        return id != null ? teams.get(id) : null;
    }

    public void createTeam(Player p, int id) {
        if (id < 1 || id > 999 || teams.containsKey(id)) return;
        Team team = new Team(p, id);
        teams.put(id, team);
        playerToTeam.put(p.getUniqueId(), id);
        updateDisplay(p, id);
    }

    public void joinTeam(Player p, int id) {
        Team t = teams.get(id);
        if (t != null && t.getMembers().size() < 3) {
            t.getMembers().add(p.getUniqueId());
            playerToTeam.put(p.getUniqueId(), id);
            updateDisplay(p, id);
        }
    }

    public void disband(int id) {
        Team t = teams.remove(id);
        if (t != null) {
            for (UUID uuid : t.getMembers()) {
                playerToTeam.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setDisplayName(p.getName());
                    p.setPlayerListName(p.getName());
                }
            }
        }
    }

    public void updateDisplay(Player p, int id) {
        String tag = "§8[§f" + id + "§8] §r";
        p.setDisplayName(tag + p.getName());
        p.setPlayerListName(tag + p.getName());
    }

    public void applyToTeam(Player source, PotionEffect effect) {
        Team t = getTeam(source);
        if (t == null) return;
        for (UUID uuid : t.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.addPotionEffect(effect);
        }
    }
}