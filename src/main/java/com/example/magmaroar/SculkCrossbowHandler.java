package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SculkCrossbowHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private final List<Arrow> trackedArrows = new ArrayList<>();

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        Player player = (Player) arrow.getShooter();
        
        // Проверяем, что игрок держит скалковый арбалет
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.CROSSBOW) return;
        if (!isSculkCrossbow(item)) return;
        
        // Проверка кулдауна
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        
        if (lastUse != null && now - lastUse < COOLDOWN) {
            long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
            player.sendMessage("§cСкалковый арбалет перезаряжается! Осталось: " + secondsLeft + " сек.");
            event.setCancelled(true);
            return;
        }
        
        // Отменяем оригинальную стрелу
        event.setCancelled(true);
        arrow.remove();
        
        // Спавним 3 стрелы
        Location eyeLoc = player.getEyeLocation();
        
        for (int i = 0; i < 3; i++) {
            Vector direction = player.getLocation().getDirection().normalize();
            
            // Добавляем небольшой разброс для стрел (как тройной выстрел)
            if (i == 0) {
                // Центральная стрела - прямо
                direction = direction.clone();
            } else if (i == 1) {
                // Левая стрела
                direction = rotateVector(direction.clone(), -0.1);
            } else if (i == 2) {
                // Правая стрела
                direction = rotateVector(direction.clone(), 0.1);
            }
            
            Arrow newArrow = player.getWorld().spawn(eyeLoc, Arrow.class);
            newArrow.setVelocity(direction.multiply(3.0));
            newArrow.setShooter(player);
            newArrow.setGlowing(true);
            newArrow.setCritical(true);
            
            // Добавляем частицы скалка
            newArrow.getWorld().spawnParticle(Particle.SCULK_SOUL, newArrow.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
            
            trackedArrows.add(newArrow);
        }
        
        // Звук выстрела
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
        
        // Ставим кулдаун
        cooldowns.put(player.getUniqueId(), now);
        player.sendMessage("§3Скалковый арбалет выпустил 3 стрелы! Кулдаун 2 минуты.");
    }

    private Vector rotateVector(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        if (!trackedArrows.contains(arrow)) return;
        
        trackedArrows.remove(arrow);
        
        Location hitLoc = arrow.getLocation();
        World world = hitLoc.getWorld();
        Player shooter = (Player) arrow.getShooter();
        
        // МОЩНЫЙ ВЗРЫВ
        world.createExplosion(hitLoc, 8.0f, false, false, shooter);
        
        // Эффекты скалка
        world.spawnParticle(Particle.SCULK_SOUL, hitLoc, 100, 3, 2, 3, 0.1);
        world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 20, 2, 1, 2, 0);
        
        // Звук
        world.playSound(hitLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);
        
        // Убираем стрелу
        arrow.remove();
    }

    private boolean isSculkCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Скалковый арбалет");
    }
}