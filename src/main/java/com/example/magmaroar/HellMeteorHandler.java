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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HellMeteorHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, UUID> meteorOwners = new HashMap<>();
    private final Set<UUID> charging = new HashSet<>();
    
    private static final long COOLDOWN = 60 * 1000; // 60 секунд
    private static final int METEOR_HEIGHT = 30; // Выше для более эффектного падения
    private static final float EXPLOSION_POWER = 10.0f;
    private static final int FIRE_RADIUS = 5;
    private static final double METEOR_SCALE = 5.0; // Размер метеорита x10

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isHellMeteor(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            if (charging.contains(player.getUniqueId())) {
                player.sendMessage("§cМетеорит уже падает!");
                event.setCancelled(true);
                return;
            }
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cАдский метеорит перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);
            Location spawnLoc = targetLoc.clone().add(0, METEOR_HEIGHT, 0);
            
            World world = player.getWorld();
            
            world.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.5f);
            player.sendMessage("§cАдский метеорит падает! (2 секунды)");
            
            charging.add(player.getUniqueId());

            // СОЗДАЁМ БОЛЬШОЙ ОГНЕННЫЙ ШАР
            LargeFireball meteor = world.spawn(spawnLoc, LargeFireball.class);
            meteor.setVelocity(new Vector(0, -1.2, 0)); // Быстрее падает
            meteor.setYield(0);
            meteor.setIsIncendiary(false);
            meteor.setGlowing(true);

            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (meteor == null || meteor.isDead() || meteor.isOnGround() || ticks >= 40) {
                        
                        if (meteor != null && !meteor.isDead()) {
                            Location hitLoc = meteor.getLocation();
                            
                            // ВЗРЫВ С УРОНОМ ПО СУЩНОСТЯМ
                            world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, player);
                            
                            // ВИЗУАЛ ВЗРЫВА (УСИЛЕННЫЙ)
                            world.spawnParticle(Particle.EXPLOSION, hitLoc, 50, 6, 6, 6, 0);
                            world.spawnParticle(Particle.FLASH, hitLoc, 30, 6, 6, 6, 0);
                            world.spawnParticle(Particle.LAVA, hitLoc, 500, 7, 5, 7, 0.15);
                            world.spawnParticle(Particle.FLAME, hitLoc, 600, 8, 6, 8, 0.08);
                            world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 80, 7, 5, 7, 0);
                            
                            world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.5f);
                            world.playSound(hitLoc, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.8f);
                            
                            // ПОДЖИГАЕМ БЛОКИ (7x7 для большей эффектности)
                            for (int x = -FIRE_RADIUS; x <= FIRE_RADIUS; x++) {
                                for (int z = -FIRE_RADIUS; z <= FIRE_RADIUS; z++) {
                                    if (Math.sqrt(x*x + z*z) <= FIRE_RADIUS + 0.5) {
                                        Location fireLoc = hitLoc.clone().add(x, 0, z);
                                        if (fireLoc.getBlock().getType() == Material.AIR) {
                                            fireLoc.getBlock().setType(Material.FIRE);
                                        }
                                    }
                                }
                            }
                            
                            // ДОПОЛНИТЕЛЬНЫЙ ОГОНЬ ВЫШЕ
                            for (int x = -2; x <= 2; x++) {
                                for (int z = -2; z <= 2; z++) {
                                    Location fireLoc = hitLoc.clone().add(x, 1, z);
                                    if (fireLoc.getBlock().getType() == Material.AIR) {
                                        fireLoc.getBlock().setType(Material.FIRE);
                                    }
                                }
                            }
                            
                            // СПАВНИМ 4 ВИЗЕР-СКЕЛЕТОВ
                            for (int i = 0; i < 4; i++) {
                                double angle = 2 * Math.PI * i / 4;
                                double x = Math.cos(angle) * 3;
                                double z = Math.sin(angle) * 3;
                                
                                Location skeletonLoc = hitLoc.clone().add(x, 0, z);
                                
                                // Проверяем, что место безопасно для спавна
                                skeletonLoc.getWorld().getChunkAt(skeletonLoc).load();
                                
                                WitherSkeleton wither = world.spawn(skeletonLoc, WitherSkeleton.class);
                                wither.setTarget(null);
                                wither.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
                                wither.setHealth(40);
                                wither.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                                wither.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                                wither.setRemoveWhenFarAway(false);
                                wither.setPersistent(true);
                                
                                meteorOwners.put(wither.getUniqueId(), player.getUniqueId());
                            }
                            
                            meteor.remove();
                        }
                        
                        cooldowns.put(player.getUniqueId(), now);
                        charging.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }
                    
                    // ЧАСТИЦЫ ДЛЯ УВЕЛИЧЕНИЯ МЕТЕОРИТА (x10)
                    double progress = (double) ticks / 40.0;
                    double currentScale = METEOR_SCALE * (1 - progress * 0.3); // Немного уменьшается к земле
                    
                    for (int i = 0; i < 40; i++) {
                        double offsetX = (Math.random() - 0.5) * currentScale * 2;
                        double offsetY = (Math.random() - 0.5) * currentScale * 2;
                        double offsetZ = (Math.random() - 0.5) * currentScale * 2;
                        
                        Location particleLoc = meteor.getLocation().clone().add(offsetX, offsetY, offsetZ);
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                        
                        if (i % 3 == 0) {
                            world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                    
                    // ВНЕШНЯЯ ОБОЛОЧКА (для эффекта размера)
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double radius = currentScale;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        Location edgeLoc = meteor.getLocation().clone().add(x, 0, z);
                        world.spawnParticle(Particle.SONIC_BOOM, edgeLoc, 1, 0, 0, 0, 0);
                    }
                    
                    // ХВОСТ (увеличенный)
                    for (int i = 0; i < 12; i++) {
                        double trailOffset = i * 0.8;
                        Location trailLoc = meteor.getLocation().clone().subtract(0, trailOffset, 0);
                        
                        world.spawnParticle(Particle.FLAME, trailLoc, 10, 1.5, 1.5, 1.5, 0.03);
                        world.spawnParticle(Particle.LAVA, trailLoc, 5, 1.2, 1.2, 1.2, 0.02);
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, trailLoc, 15, 2.0, 2.0, 2.0, 0.02);
                    }
                    
                    // Звук падения
                    if (ticks % 8 == 0) {
                        world.playSound(meteor.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.6f);
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof WitherSkeleton) {
            WitherSkeleton wither = (WitherSkeleton) event.getEntity();
            UUID ownerId = meteorOwners.get(wither.getUniqueId());
            
            if (ownerId != null && event.getTarget() instanceof Player && 
                ((Player) event.getTarget()).getUniqueId().equals(ownerId)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof WitherSkeleton) {
            WitherSkeleton wither = (WitherSkeleton) event.getEntity();
            if (meteorOwners.containsKey(wither.getUniqueId())) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isHellMeteor(ItemStack item) {
        if (item == null || item.getType() != Material.BREEZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Адский метеорит");
    }
}