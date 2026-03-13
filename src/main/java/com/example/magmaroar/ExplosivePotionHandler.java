package com.example.magmaroar;

import org.bukkit.Color;
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

public class ExplosivePotionHandler implements Listener {

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        Player player = (Player) snowball.getShooter();
        
        // Проверяем, что это наше зелье
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isExplosivePotion(item)) return;
        
        // Разрешаем бросить
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player shooter = (Player) snowball.getShooter();
        
        // Даём эффекты всем вокруг
        for (LivingEntity entity : snowball.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(snowball.getLocation()) <= 5) {
                // Сила II на 3 минуты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 1));
                // Скорость II на 3 минуты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1));
            }
        }
        
        // Частицы (исправленные)
        snowball.getWorld().spawnParticle(Particle.SPELL_MOB, 
            snowball.getLocation(), 50, 1, 1, 1, 1);
        
        // Дополнительные фиолетовые частицы
        snowball.getWorld().spawnParticle(Particle.END_ROD, 
            snowball.getLocation(), 30, 1, 1, 1, 0.1);
    }

    private boolean isExplosivePotion(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Взрывное зелье");
    }
}