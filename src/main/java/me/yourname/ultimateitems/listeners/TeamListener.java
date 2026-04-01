package me.yourname.ultimateitems.listeners;

import me.yourname.ultimateitems.UltimateItems;
import me.yourname.ultimateitems.teams.Team;
import me.yourname.ultimateitems.teams.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class TeamListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TeamManager tm = UltimateItems.getInstance().getTeamManager();
        
        // Проверяем, есть ли игрок в какой-то команде (если данные сохраняются в конфиг)
        Team team = tm.getTeam(player);
        if (team != null) {
            tm.updateDisplay(player, team.getId());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        TeamManager tm = UltimateItems.getInstance().getTeamManager();
        Team team = tm.getTeam(player);

        if (team != null) {
            // Формат чата: [123] Ник: Сообщение
            String format = "§8[§f" + team.getId() + "§8] §r%1$s: %2$s";
            event.setFormat(format);
        }
    }
}