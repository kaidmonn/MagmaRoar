package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private final Set<UUID> meteorProjectiles = new HashSet<>();
    private final Map<UUID, UUID> meteorOwners = new HashMap<>();
    
    private static final long COOLDOWN = 60 * 1000;
    private static final int METEOR_HEIGHT = 25;
    private static final float EXPLOSION_POWER = 10.0f;
    private static final int FIRE_RADIUS = 5;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isHellMeteor(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cАдский метеорит перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 0, 0.5);
            
            Vector direction = player.getLocation().getDirection().normalize();
            double horizontalDistance = 15.0;
            Location spawnLoc = targetLoc.clone().add(
                direction.getX() * horizontalDistance,
                METEOR_HEIGHT,
                direction.getZ() * horizontalDistance
            );
            
            World world = player.getWorld();
            
            world.playSound(spawnLoc, Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.5f);
            
            LargeFireball meteor = world.spawn(spawnLoc, LargeFireball.class);
            
            Vector toTarget = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
            meteor.setVelocity(toTarget.multiply(1.2));
            meteor.setYield(0);
            meteor.setIsIncendiary(false);
            meteor.setGlowing(true);
            
            meteor.setCustomName("§c§lМЕТЕОРИТ");
            meteor.setCustomNameVisible(true);
            
            meteorProjectiles.add(meteor.getUniqueId());
            meteorOwners.put(meteor.getUniqueId(), player.getUniqueId());
            
            cooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§cАдский метеорит падает под углом! (2 секунды)");
            event.setCancelled(true);

            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (meteor == null || meteor.isDead() || meteor.isOnGround() || ticks >= 40) {
                        
                        if (meteor != null && !meteor.isDead()) {
                            Location hitLoc = meteor.getLocation();
                            
                            // ВИЗУАЛ ВЗРЫВА
                            world.spawnParticle(Particle.EXPLOSION, hitLoc, 20, 3, 3, 3, 0);
                            world.spawnParticle(Particle.FLASH, hitLoc, 10, 2, 2, 2, 0);
                            
                            // ОГНЕННЫЙ КРУГ 5×5
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
                            
                            world.createExplosion(hitLoc, EXPLOSION_POWER, true, true, player);
                            
                            world.spawnParticle(Particle.LAVA, hitLoc, 200, FIRE_RADIUS, 3, FIRE_RADIUS, 0.1);
                            world.spawnParticle(Particle.FLAME, hitLoc, 300, FIRE_RADIUS, 4, FIRE_RADIUS, 0.05);
                            world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 50, 5, 3, 5, 0);
                            
                            world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                            world.playSound(hitLoc, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.8f);
                            
                            for (int i = 0; i < 4; i++) {
                                Location spawnLoc = hitLoc.clone().add(
                                    (Math.random() - 0.5) * 5,
                                    0,
                                    (Math.random() - 0.5) * 5
                                );
                                
                                WitherSkeleton wither = world.spawn(spawnLoc, WitherSkeleton.class);
                                wither.setTarget(null);
                                wither.setAI(true);
                                wither.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(30);
                                wither.setHealth(30);
                                wither.setRemoveWhenFarAway(false);
                                wither.setPersistent(true);
                                
                                wither.addPotionEffect(new PotionEffect(
                                    PotionEffectType.FIRE_RESISTANCE, 
                                    Integer.MAX_VALUE, 
                                    0, 
                                    false, 
                                    false
                                ));
                                
                                meteorOwners.put(wither.getUniqueId(), player.getUniqueId());
                            }
                            
                            meteorProjectiles.remove(meteor.getUniqueId());
                            meteorOwners.remove(meteor.getUniqueId());
                            meteor.remove();
                        }
                        
                        this.cancel();
                    }
                    
                    if (meteor != null && !meteor.isDead()) {
                        world.spawnParticle(Particle.FLAME, meteor.getLocation(), 20, 1, 1, 1, 0.02);
                        world.spawnParticle(Particle.LAVA, meteor.getLocation(), 10, 0.5, 0.5, 0.5, 0.01);
                        world.spawnParticle(Particle.SMOKE_LARGE, meteor.getLocation(), 15, 1, 1, 1, 0.01);
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
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
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
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