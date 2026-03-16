package com.example.magmaroar;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BattleManager {

    private final MythicNPC plugin;
    private final List<Player> battlePlayers = new ArrayList<>();
    private final Location battleLocation = new Location(Bukkit.getWorld("world"), 133, -32, 93);
    private boolean battleActive = false;

    public BattleManager(MythicNPC plugin) {
        this.plugin = plugin;
    }

    public void startBattleCountdown(List<Player> players, Map<Player, Integer> positions) {
        battlePlayers.clear();
        battlePlayers.addAll(players);
        battleActive = true;

        new BukkitRunnable() {
            int seconds = 60;
            
            @Override
            public void run() {
                if (seconds <= 0) {
                    startBattle();
                    this.cancel();
                    return;
                }
                
                if (seconds == 60 || seconds == 30 || seconds == 10 || seconds == 5 || seconds == 3) {
                    for (Player p : battlePlayers) {
                        p.sendMessage("§6До начала битвы: §e" + seconds + " §6сек.");
                    }
                }
                
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startBattle() {
        for (Player player : battlePlayers) {
            player.teleport(battleLocation);
            
            player.showTitle(Title.title(
                "§c§lБОЙ НАЧАЛСЯ!",
                "§eУдачи!"
            ));
            
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);
        }
    }

    public void checkWinner(Player deadPlayer) {
        if (!battleActive) return;
        
        battlePlayers.remove(deadPlayer);
        
        if (battlePlayers.size() == 1) {
            Player winner = battlePlayers.get(0);
            
            winner.showTitle(Title.title(
                "§6§lВЫ ВЫИГРАЛИ!",
                "§eПоздравляем!"
            ));
            
            winner.playSound(winner.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
            
            winner.teleport(winner.getBedSpawnLocation() != null ? 
                winner.getBedSpawnLocation() : winner.getWorld().getSpawnLocation());
            
            battlePlayers.clear();
            battleActive = false;
        }
    }
}