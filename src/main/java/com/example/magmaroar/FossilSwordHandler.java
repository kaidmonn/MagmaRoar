package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FossilSwordHandler implements Listener {

    private final Map<UUID, Boolean> totemActive = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 60 * 1000; // 60 секунд
    private static final int TOTEM_DURATION = 20 * 20; // 20 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isFossilSword(item)) return;

        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            long now = System.currentTimeMillis();
            if (cooldowns.getOrDefault(player.getUniqueId(), 0L) > now) {
                long secondsLeft = (cooldowns.get(player.getUniqueId()) - now) / 1000;
                player.sendMessage("§cЭффект тотема перезаряжается! Осталось: " + secondsLeft + " сек.");
                return;
            }

            // Активируем эффекты тотема
            activateTotemEffects(player);
            player.sendMessage("§6§lЭФФЕКТ ТОТЕМА АКТИВИРОВАН НА 20 СЕКУНД!");
            
            // Ставим кулдаун
            cooldowns.put(player.getUniqueId(), now + COOLDOWN_TIME);
            
            // Убираем эффекты через 20 секунд
            new BukkitRunnable() {
                @Override
                public void run() {
                    deactivateTotemEffects(player);
                    player.sendMessage("§cЭффект тотема закончился.");
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), TOTEM_DURATION);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Если активен эффект тотема - отменяем смерть
        if (totemActive.getOrDefault(player.getUniqueId(), false)) {
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);
                player.setHealth(1.0);
                player.sendMessage("§d§lТотем ископаемого меча спас вас!");
                
                // Эффекты
                player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation(), 100, 1, 1, 1, 0.5);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                
                // Меч исчезает
                ItemStack sword = player.getInventory().getItemInMainHand();
                if (isFossilSword(sword)) {
                    player.getInventory().setItemInMainHand(null);
                }
                
                totemActive.put(player.getUniqueId(), false);
            }
        }
    }

    private void activateTotemEffects(Player player) {
        totemActive.put(player.getUniqueId(), true);
        
        // Добавляем эффекты
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, TOTEM_DURATION, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, TOTEM_DURATION, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, TOTEM_DURATION, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, TOTEM_DURATION, 1));
        
        // Визуал
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 1, 1, 1, 0.2);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void deactivateTotemEffects(Player player) {
        totemActive.put(player.getUniqueId(), false);
        
        // Убираем эффекты
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
    }

    private boolean isFossilSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Ископаемый меч");
    }
}