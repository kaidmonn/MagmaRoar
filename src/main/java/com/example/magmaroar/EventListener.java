package com.example.magmaroar;

import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {

    private final MagmaRoarPlugin plugin;

    public EventListener(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractNPC(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Zombie) {
            Zombie clicked = (Zombie) event.getRightClicked();
            if (plugin.getNPCManager().isNPC(clicked)) {
                plugin.getQueueManager().addToQueue(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            if (plugin.getNPCManager().isNPC(zombie)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // УБРАЛИ ВЫЗОВ АНИМАЦИИ
        // Здесь больше ничего не нужно
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getQueueManager().removeFromQueue(player);
        plugin.getBattleManager().checkWinner(player);
    }
}