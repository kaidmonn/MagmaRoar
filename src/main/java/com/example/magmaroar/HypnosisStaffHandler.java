package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HypnosisStaffHandler implements Listener {

    private final Map<UUID, WardenInfo> activeWardens = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 90 * 1000; // 90 секунд
    private static final int WARDEN_LIFETIME = 40 * 1000; // 40 секунд

    private static class WardenInfo {
        Warden warden;
        long spawnTime;
        LivingEntity target;
        
        WardenInfo(Warden warden, long spawnTime) {
            this.warden = warden;
            this.spawnTime = spawnTime;
            this.target = null;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isHypnosisStaff(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            
            // Проверяем, есть ли уже активный Варден
            WardenInfo info = activeWardens.get(player.getUniqueId());
            
            if (info != null && info.warden != null && !info.warden.isDead()) {
                // Телепортируем Вардена к игроку
                info.warden.teleport(player.getLocation());
                player.sendMessage("§5Варден телепортирован к вам!");
                event.setCancelled(true);
                return;
            }
            
            // Проверка кулдауна
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЖезл гипноза перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Призыв Вардена
            Location spawnLoc = player.getLocation();
            World world = player.getWorld();
            
            Warden warden = world.spawn(spawnLoc, Warden.class);
            
            // Настройка Вардена
            warden.setAI(true);
            warden.setTarget(null);
            warden.setHealth(500); // Максимальное здоровье
            warden.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3)); // Скорость 4
            
            // Следующая за владельцем
            warden.setTarget(player);
            
            // Сохраняем информацию
            WardenInfo newInfo = new WardenInfo(warden, now);
            activeWardens.put(player.getUniqueId(), newInfo);
            cooldowns.put(player.getUniqueId(), now);
            
            // Звук призыва
            world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
            world.playSound(spawnLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);
            
            player.sendMessage("§5Варден призван! Он будет следовать за вами 40 секунд.");
            
            // Запускаем таймер на исчезновение
            new BukkitRunnable() {
                @Override
                public void run() {
                    WardenInfo current = activeWardens.get(player.getUniqueId());
                    if (current != null && current.warden != null && !current.warden.isDead()) {
                        current.warden.remove();
                        activeWardens.remove(player.getUniqueId());
                        player.sendMessage("§5Варден исчез...");
                        
                        // Звук исчезновения
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), WARDEN_LIFETIME / 50);
            
            // Запускаем таймер на следование и атаку
            new BukkitRunnable() {
                @Override
                public void run() {
                    WardenInfo current = activeWardens.get(player.getUniqueId());
                    if (current == null || current.warden == null || current.warden.isDead()) {
                        this.cancel();
                        return;
                    }
                    
                    Warden warden = current.warden;
                    
                    // Следование за владельцем (если нет цели)
                    if (current.target == null || current.target.isDead()) {
                        if (warden.getLocation().distance(player.getLocation()) > 10) {
                            warden.setTarget(player);
                        } else {
                            warden.setTarget(null);
                        }
                    } else {
                        // Атака цели
                        warden.setTarget(current.target);
                        
                        // Если цель слишком далеко от владельца, забываем её
                        if (current.target.getLocation().distance(player.getLocation()) > 15) {
                            current.target = null;
                            warden.setTarget(player);
                        }
                    }
                    
                    // Частицы связи с владельцем
                    player.getWorld().spawnParticle(Particle.SCULK_SOUL, 
                        warden.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.02);
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 10L);
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isHypnosisStaff(item)) return;
        
        // Устанавливаем цель для Вардена
        WardenInfo info = activeWardens.get(player.getUniqueId());
        if (info != null && info.warden != null && !info.warden.isDead()) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                info.target = target;
                
                // Сообщение
                player.sendMessage("§5Варден атакует: " + (target instanceof Player ? target.getName() : "моб"));
                
                // Эффект на цели
                target.getWorld().spawnParticle(Particle.SCULK_SOUL, 
                    target.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.1);
            }
        }
    }

    private boolean isHypnosisStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Жезл гипноза");
    }
}