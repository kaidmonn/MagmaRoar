package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class BattleManager {

    private final MagmaRoarPlugin plugin;
    private final List<Player> battlePlayers = new ArrayList<>();
    private final Location battleLocation = new Location(Bukkit.getWorld("world"), 133, -32, 93);
    private boolean battleActive = false;

    public BattleManager(MagmaRoarPlugin plugin) {
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
                Component.text("§c§lБОЙ НАЧАЛСЯ!"),
                Component.text("§eУдачи!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
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
                Component.text("§6§lВЫ ВЫИГРАЛИ!"),
                Component.text("§eПоздравляем!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
            
            winner.playSound(winner.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
            
            // Очищаем инвентарь победителя
            winner.getInventory().clear();
            winner.getInventory().setHelmet(null);
            winner.getInventory().setChestplate(null);
            winner.getInventory().setLeggings(null);
            winner.getInventory().setBoots(null);
            winner.getInventory().setItemInOffHand(null);
            
            // УДАЛЯЕМ ВСЕ ПРЕДМЕТЫ НА ЗЕМЛЕ В РАДИУСЕ 30 БЛОКОВ
            for (Entity entity : winner.getWorld().getNearbyEntities(battleLocation, 30, 30, 30)) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
            
            winner.sendMessage("§aВаш инвентарь очищен! Все предметы на земле удалены.");
            
            winner.teleport(winner.getBedSpawnLocation() != null ? 
                winner.getBedSpawnLocation() : winner.getWorld().getSpawnLocation());
            
            battlePlayers.clear();
            battleActive = false;
        }
    }
}