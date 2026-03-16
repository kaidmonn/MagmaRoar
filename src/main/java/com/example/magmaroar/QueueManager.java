package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class QueueManager {

    private final MythicNPC plugin;
    private final List<Player> queue = new ArrayList<>();
    private final List<Player> confirmedPlayers = new ArrayList<>();
    private final Map<Player, Integer> playerPositions = new HashMap<>();
    private boolean queueActive = false;
    private int taskId = -1;
    
    private final Location[] battleLocations = {
        new Location(Bukkit.getWorld("world"), 8, -52, -6),
        new Location(Bukkit.getWorld("world"), 8, -52, -2),
        new Location(Bukkit.getWorld("world"), 8, -52, 1),
        new Location(Bukkit.getWorld("world"), 8, -52, 5),
        new Location(Bukkit.getWorld("world"), 8, -52, 9)
    };

    public QueueManager(MythicNPC plugin) {
        this.plugin = plugin;
    }

    public void addToQueue(Player player) {
        if (queue.size() >= 5) {
            player.sendMessage("§cОчередь заполнена! Максимум 5 игроков.");
            return;
        }

        if (queue.contains(player)) {
            player.sendMessage("§cВы уже в очереди!");
            return;
        }

        queue.add(player);
        player.sendMessage("§aВы добавлены в очередь! §7(§f" + queue.size() + "§7/§f5§7)");
        
        for (Player p : queue) {
            p.sendMessage("§eВ очереди: §f" + queue.size() + "§7/§f5");
        }
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

        if (queue.size() >= 2 && !queueActive) {
            startQueueCountdown();
        }
    }

    private void startQueueCountdown() {
        queueActive = true;
        
        for (Player p : queue) {
            p.sendMessage("§6§lНабор в очередь начнётся через 20 секунд!");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        taskId = new BukkitRunnable() {
            int seconds = 20;
            
            @Override
            public void run() {
                if (seconds <= 0) {
                    startBattlePreparation();
                    this.cancel();
                    return;
                }
                
                if (seconds == 10 || seconds == 5 || seconds == 3 || seconds == 2 || seconds == 1) {
                    for (Player p : queue) {
                        p.sendMessage("§eДо начала: §f" + seconds + " §eсек.");
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                    }
                }
                
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private void startBattlePreparation() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        confirmedPlayers.clear();
        playerPositions.clear();
        
        // Телепортируем игроков на места
        for (int i = 0; i < Math.min(queue.size(), 5); i++) {
            Player player = queue.get(i);
            player.teleport(battleLocations[i]);
            confirmedPlayers.add(player);
            playerPositions.put(player, i);
            
            player.sendMessage("§aВы на позиции " + (i + 1) + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }

        // Запускаем выдачу китов через 20 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getKitManager().giveKits(confirmedPlayers);
                plugin.getBattleManager().startBattleCountdown(confirmedPlayers, playerPositions);
            }
        }.runTaskLater(plugin, 20 * 20); // 20 секунд
        
        queueActive = false;
    }

    public void removeFromQueue(Player player) {
        queue.remove(player);
    }

    public List<Player> getQueue() {
        return queue;
    }
}