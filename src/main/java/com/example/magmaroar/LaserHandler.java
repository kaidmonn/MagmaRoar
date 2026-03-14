package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> charging = new HashMap<>();
    private final Map<UUID, LivingEntity> lockedTarget = new HashMap<>();
    
    private static final long COOLDOWN = 60 * 1000; // 60 секунд
    private static final int AIM_TIME = 60; // 3 секунды на прицеливание
    private static final int LASER_DURATION = 200; // 10 секунд лазера
    private static final int FIRE_DURATION = 400; // 20 секунд огня
    private static final int DAMAGE_INTERVAL = 20; // 1 секунда между уроном
    private static final int MAX_RANGE = 100; // Максимальная дистанция

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLaser(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            
            if (charging.containsKey(player.getUniqueId())) {
                player.sendMessage("§cЛазер уже активируется!");
                event.setCancelled(true);
                return;
            }
            
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЛазер перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            player.sendMessage("§eЛазер активирован! Наведи на цель (3 секунды)...");
            
            charging.put(player.getUniqueId(), true);

            new BukkitRunnable() {
                int aimTicks = 0;
                LivingEntity target = null;
                
                @Override
                public void run() {
                    // Проверяем, не передумал ли игрок
                    if (!charging.containsKey(player.getUniqueId())) {
                        this.cancel();
                        return;
                    }
                    
                    // Ищем цель в прицеле
                    LivingEntity newTarget = getTargetEntity(player, MAX_RANGE);
                    
                    // Визуальный эффект прицеливания
                    if (newTarget != null) {
                        Location targetLoc = newTarget.getLocation().add(0, 1, 0);
                        player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 10, 0.3, 0.5, 0.3, 0.02);
                        
                        // Если цель сменилась, сбрасываем таймер
                        if (target != newTarget) {
                            target = newTarget;
                            aimTicks = 0;
                        }
                    }
                    
                    aimTicks++;
                    
                    // Отображаем прогресс
                    if (aimTicks % 10 == 0) {
                        int secondsLeft = (AIM_TIME - aimTicks) / 20 + 1;
                        player.sendMessage("§eОсталось: " + secondsLeft + " сек. Цель: " + 
                            (target != null ? target.getName() : "не найдена"));
                    }
                    
                    // Если прицеливание завершено и цель есть
                    if (aimTicks >= AIM_TIME) {
                        charging.remove(player.getUniqueId());
                        
                        if (target != null && !target.isDead()) {
                            startLaser(player, target);
                            player.sendMessage("§eЛазер активирован! Цель поражена.");
                        } else {
                            player.sendMessage("§cЦель не найдена! Лазер не активирован.");
                            cooldowns.put(player.getUniqueId(), now); // Всё равно ставим кулдаун
                        }
                        
                        this.cancel();
                    }
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
            
            event.setCancelled(true);
        }
    }

    private void startLaser(Player player, LivingEntity target) {
        World world = player.getWorld();
        UUID targetId = target.getUniqueId();
        
        // Запоминаем цель
        lockedTarget.put(player.getUniqueId(), target);
        
        // Мгновенно поджигаем
        target.setFireTicks(FIRE_DURATION);
        
        // Звук активации
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int laserTicks = 0;
            
            @Override
            public void run() {
                // Проверяем, жива ли цель
                if (target == null || target.isDead() || laserTicks >= LASER_DURATION) {
                    lockedTarget.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                Location playerLoc = player.getEyeLocation();
                Location targetLoc = target.getLocation().add(0, 1, 0);
                
                // Проверяем дистанцию
                if (playerLoc.distance(targetLoc) > MAX_RANGE) {
                    player.sendMessage("§cЦель слишком далеко! Лазер отключён.");
                    lockedTarget.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Проверяем прямую видимость
                if (!player.hasLineOfSight(target)) {
                    player.sendMessage("§cНет прямой видимости! Лазер отключён.");
                    lockedTarget.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // РИСУЕМ ТОЛСТЫЙ ЛАЗЕР
                Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
                double distance = playerLoc.distance(targetLoc);
                
                for (double d = 0; d < distance; d += 1) {
                    Vector step = direction.clone().multiply(d);
                    Location beamLoc = playerLoc.clone().add(step);
                    
                    // Толстый луч (несколько частиц)
                    for (int i = 0; i < 5; i++) {
                        double offsetX = (Math.random() - 0.5) * 1.5;
                        double offsetY = (Math.random() - 0.5) * 1.5;
                        double offsetZ = (Math.random() - 0.5) * 1.5;
                        
                        Location particleLoc = beamLoc.clone().add(offsetX, offsetY, offsetZ);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    }
                    
                    // Дополнительные частицы для толщины
                    if (d % 2 == 0) {
                        world.spawnParticle(Particle.FLASH, beamLoc, 1, 0.5, 0.5, 0.5, 0);
                    }
                }
                
                // УРОН КАЖДУЮ СЕКУНДУ
                if (laserTicks % DAMAGE_INTERVAL == 0 && laserTicks > 0) {
                    target.damage(2.0, player); // 1 сердце
                    world.playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                }
                
                // ПОДДЕРЖИВАЕМ ОГОНЬ
                target.setFireTicks(FIRE_DURATION);
                
                // Звук лазера
                if (laserTicks % 10 == 0) {
                    world.playSound(targetLoc, Sound.ENTITY_BLAZE_BURN, 0.5f, 1.5f);
                }
                
                laserTicks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        
        // Запускаем таймер на снятие блокировки
        new BukkitRunnable() {
            @Override
            public void run() {
                lockedTarget.remove(player.getUniqueId());
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), LASER_DURATION);
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        
        for (int i = 0; i < range; i++) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(i));
            
            for (Entity e : checkLoc.getWorld().getNearbyEntities(checkLoc, 1.5, 1.5, 1.5)) {
                if (e instanceof LivingEntity && !e.equals(player)) {
                    return (LivingEntity) e;
                }
            }
        }
        return null;
    }

    private boolean isLaser(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лазер");
    }
} 