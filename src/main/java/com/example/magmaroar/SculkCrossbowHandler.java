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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

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

        // Запускаем 3 стрелы с задержкой
        player.sendMessage("§3Скалковый арбалет заряжается...");
        
        new BukkitRunnable() {
            int arrowsShot = 0;
            
            @Override
            public void run() {
                if (arrowsShot >= 3) {
                    this.cancel();
                    cooldowns.put(player.getUniqueId(), now);
                    player.sendMessage("§3Скалковый арбалет выпустил 3 стрелы! Кулдаун 2 минуты.");
                    return;
                }
                
                // Спавним стрелу
                Location eyeLoc = player.getEyeLocation();
                Vector direction = player.getLocation().getDirection().normalize();
                
                // Добавляем небольшой разброс для третьей стрелы
                if (arrowsShot == 1) {
                    direction = direction.clone().add(new Vector(0.1, 0, 0.1)).normalize();
                } else if (arrowsShot == 2) {
                    direction = direction.clone().add(new Vector(-0.1, 0, -0.1)).normalize();
                }
                
                Arrow arrow = player.getWorld().spawn(eyeLoc, Arrow.class);
                arrow.setVelocity(direction.multiply(4.0)); // Очень быстро
                arrow.setShooter(player);
                arrow.setGlowing(true);
                
                // Вместо setPierce используем setCritical
                arrow.setCritical(true);
                
                // Добавляем частицы скалка
                arrow.getWorld().spawnParticle(Particle.SCULK_SOUL, arrow.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                
                trackedArrows.add(arrow);
                arrowsShot++;
                
                // Звук выстрела
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CROSSBOW_SHOOT, 1.0f, 0.5f);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L); // Стрелы с интервалом в 5 тиков
        
        event.setCancelled(true);
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
        
        // МОЩНЫЙ ВЗРЫВ (урон 40-50, убивает в полном незерите)
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