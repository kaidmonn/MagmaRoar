package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExplosivePotionHandler implements Listener {

    private final Set<UUID> explosiveSnowballs = new HashSet<>(); // Только наши зелья

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        Player player = (Player) snowball.getShooter();
        
        // Проверяем, что это НАШЕ взрывное зелье
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isExplosivePotion(item)) return;
        
        // Запоминаем, что это наше зелье
        explosiveSnowballs.add(snowball.getUniqueId());
        
        // Визуальный эффект при броске
        snowball.setGlowing(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        
        // Проверяем, наше ли это зелье
        if (!explosiveSnowballs.contains(snowball.getUniqueId())) return;
        
        // Удаляем из памяти
        explosiveSnowballs.remove(snowball.getUniqueId());
        
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player shooter = (Player) snowball.getShooter();
        
        // Даём эффекты всем вокруг
        for (LivingEntity entity : snowball.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(snowball.getLocation()) <= 5) {
                // Сила II на 3 минуты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 1, true, true, true));
                // Скорость II на 3 минуты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1, true, true, true));
            }
        }
        
        // Частицы
        snowball.getWorld().spawnParticle(Particle.END_ROD, snowball.getLocation(), 50, 1, 1, 1, 0.1);
        snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 100, 1, 1, 1, 0.5);
        
        // Звук
        snowball.getWorld().playSound(snowball.getLocation(), org.bukkit.Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
    }

    private boolean isExplosivePotion(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Взрывное зелье");
    }
}