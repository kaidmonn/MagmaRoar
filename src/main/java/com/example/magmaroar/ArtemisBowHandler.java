package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ArtemisBowHandler implements Listener {

    private final Random random = new Random();
    private final Map<UUID, ArtemisArrow> activeArrows = new HashMap<>();
    
    private static final double ARROW_DAMAGE = 12.0;
    private static final int HOMING_RADIUS = 70;
    private static final double HOMING_CHANCE = 0.5; // 50%
    private static final float EXPLOSION_POWER = 4.0f;
    private static final int FIRE_RADIUS = 5;
    private static final int FIRE_DURATION = 100; // 5 секунд

    private static class ArtemisArrow {
        UUID arrowId;
        ArrowType type;
        boolean homing;
        LivingEntity target;

        ArtemisArrow(UUID arrowId, ArrowType type, boolean homing, LivingEntity target) {
            this.arrowId = arrowId;
            this.type = type;
            this.homing = homing;
            this.target = target;
        }
    }

    private enum ArrowType {
        EXPLOSIVE,  // Взрывная (10%)
        FIRE,       // Огненная (25%)
        LIGHTNING,  // Молниевая (65%)
        ROYAL       // Королевская (5%)
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();

        if (!isArtemisBow(bow)) return;

        Arrow arrow = (Arrow) event.getProjectile();
        
        // Устанавливаем урон
        arrow.setDamage(ARROW_DAMAGE);
        
        // Определяем тип стрелы
        ArrowType type = determineArrowType();
        
        // Определяем самонаводку
        boolean homing = random.nextDouble() < HOMING_CHANCE;
        if (type == ArrowType.ROYAL) homing = true; // Королевская всегда с самонаводкой
        
        // Ищем цель для самонаводки
        LivingEntity target = null;
        if (homing) {
            target = findNearestTarget(player, HOMING_RADIUS);
        }
        
        // Сохраняем информацию о стреле
        activeArrows.put(arrow.getUniqueId(), new ArtemisArrow(
            arrow.getUniqueId(), type, homing, target
        ));
        
        // Визуальный эффект при выстреле
        spawnShootParticles(player, type);
        
        // Запускаем таск для самонаводки
        if (homing && target != null) {
            startHomingTask(arrow, target);
        }
    }

    private ArrowType determineArrowType() {
        double r = random.nextDouble() * 100;
        
        if (r < 5) { // 5% королевская
            return ArrowType.ROYAL;
        } else if (r < 30) { // 25% огненная (5-30)
            return ArrowType.FIRE;
        } else if (r < 95) { // 65% молниевая (30-95)
            return ArrowType.LIGHTNING;
        } else { // 5% взрывная (95-100)
            return ArrowType.EXPLOSIVE;
        }
    }

    private LivingEntity findNearestTarget(Player shooter, int radius) {
        Location shooterLoc = shooter.getLocation();
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity e : shooter.getWorld().getEntities()) {
            if (e instanceof LivingEntity && !e.equals(shooter)) {
                LivingEntity le = (LivingEntity) e;
                double distance = le.getLocation().distance(shooterLoc);
                
                if (distance <= radius && distance < nearestDistance) {
                    nearest = le;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private void startHomingTask(Arrow arrow, LivingEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || target.isDead()) {
                    this.cancel();
                    return;
                }
                
                // Вычисляем направление к цели
                Vector arrowVel = arrow.getVelocity();
                Vector toTarget = target.getLocation().add(0, 1, 0).toVector()
                    .subtract(arrow.getLocation().toVector()).normalize();
                
                // Плавно поворачиваем стрелу
                Vector newVel = arrowVel.add(toTarget.multiply(0.1)).normalize()
                    .multiply(arrowVel.length());
                
                arrow.setVelocity(newVel);
                
                // Частицы следа
                arrow.getWorld().spawnParticle(Particle.END_ROD, 
                    arrow.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void spawnShootParticles(Player player, ArrowType type) {
        Particle particle = Particle.END_ROD;
        switch (type) {
            case EXPLOSIVE:
                particle = Particle.FLAME;
                break;
            case FIRE:
                particle = Particle.FLAME;
                break;
            case LIGHTNING:
                particle = Particle.ELECTRIC_SPARK;
                break;
            case ROYAL:
                particle = Particle.END_ROD;
                player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);
                break;
        }
        
        player.getWorld().spawnParticle(particle, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        ArtemisArrow info = activeArrows.remove(arrow.getUniqueId());
        
        if (info == null) return;
        
        Location hitLoc = arrow.getLocation();
        World world = hitLoc.getWorld();
        
        // Эффекты в зависимости от типа
        switch (info.type) {
            case EXPLOSIVE:
                // Взрыв уровня 4
                world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, null);
                world.spawnParticle(Particle.EXPLOSION, hitLoc, 30, 2, 2, 2, 0);
                world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                break;
                
            case FIRE:
                // Поджог 5x5
                for (int x = -FIRE_RADIUS; x <= FIRE_RADIUS; x++) {
                    for (int z = -FIRE_RADIUS; z <= FIRE_RADIUS; z++) {
                        if (Math.sqrt(x*x + z*z) <= FIRE_RADIUS) {
                            Location fireLoc = hitLoc.clone().add(x, 0, z);
                            if (fireLoc.getBlock().getType() == Material.AIR) {
                                fireLoc.getBlock().setType(Material.FIRE);
                            }
                        }
                    }
                }
                world.playSound(hitLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                break;
                
            case LIGHTNING:
                // Ванильная молния
                world.strikeLightning(hitLoc);
                break;
                
            case ROYAL:
                // ВСЁ СРАЗУ!
                world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, null);
                
                for (int x = -FIRE_RADIUS; x <= FIRE_RADIUS; x++) {
                    for (int z = -FIRE_RADIUS; z <= FIRE_RADIUS; z++) {
                        if (Math.sqrt(x*x + z*z) <= FIRE_RADIUS) {
                            Location fireLoc = hitLoc.clone().add(x, 0, z);
                            if (fireLoc.getBlock().getType() == Material.AIR) {
                                fireLoc.getBlock().setType(Material.FIRE);
                            }
                        }
                    }
                }
                
                world.strikeLightning(hitLoc);
                
                // Золотые частицы
                world.spawnParticle(Particle.END_ROD, hitLoc, 100, 3, 3, 3, 0.1);
                world.spawnParticle(Particle.FLASH, hitLoc, 20, 2, 2, 2, 0);
                world.playSound(hitLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                break;
        }
        
        // Удаляем стрелу
        arrow.remove();
    }

    private boolean isArtemisBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лук Артемиды");
    }
}