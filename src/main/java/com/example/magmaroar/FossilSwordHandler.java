package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FossilSwordHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 75 * 1000; // 75 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isFossilSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cИскопаемый меч перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Даём все эффекты тотема на 20 секунд
            giveTotemEffects(player, 20 * 20); // 20 секунд в тиках
            
            player.sendMessage("§6§lИСКОПАЕМЫЙ МЕЧ! Эффекты тотема на 20 секунд!");
            
            // Визуал и звук
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 0.5, 1, 0.5, 0.5);
            
            cooldowns.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Проверяем, что урон смертельный
        if (player.getHealth() - event.getFinalDamage() > 0) return;
        
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        // Проверяем, есть ли меч в руках
        boolean hasInMain = isFossilSword(mainHand);
        boolean hasInOff = isFossilSword(offHand);
        
        if (hasInMain || hasInOff) {
            // Отменяем смерть
            event.setCancelled(true);
            
            // Лечим игрока
            player.setHealth(player.getMaxHealth() / 2); // Половина здоровья
            
            // Даём эффекты тотема
            giveTotemEffects(player, 40 * 20); // 40 секунд в тиках
            
            // Эффекты тотема
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 200, 0.5, 1, 0.5, 0.5);
            
            player.sendMessage("§6§lИСКОПАЕМЫЙ МЕЧ СПАС ТЕБЕ ЖИЗНЬ!");
            
            // Удаляем меч (тот, в котором он был)
            if (hasInMain) {
                if (mainHand.getAmount() > 1) {
                    mainHand.setAmount(mainHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            } else {
                if (offHand.getAmount() > 1) {
                    offHand.setAmount(offHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
    }

    private void giveTotemEffects(Player player, int duration) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, true, true));
    }

    private boolean isFossilSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Ископаемый меч");
    }
}