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
    private final Map<UUID, UUID> meteorOwners = new HashMap<>();
    private final Set<UUID> charging = new HashSet<>();
    
    private static final long COOLDOWN = 60 * 1000;
    private static final int METEOR_HEIGHT = 25;
    private static final float EXPLOSION_POWER = 10.0f;

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
            meteor.setVelocity(new Vector(0, -0.8, 0));
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
                            
                            // ВЗРЫВ (БЕЗ РАЗРУШЕНИЯ БЛОКОВ)
                            world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, player);
                            
                            // ВИЗУАЛ ВЗРЫВА
                            world.spawnParticle(Particle.EXPLOSION, hitLoc, 30, 4, 4, 4, 0);
                            world.spawnParticle(Particle.FLASH, hitLoc, 20, 4, 4, 4, 0);
                            world.spawnParticle(Particle.LAVA, hitLoc, 300, 5, 4, 5, 0.1);
                            world.spawnParticle(Particle.FLAME, hitLoc, 400, 6, 5, 6, 0.05);
                            
                            world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                            world.playSound(hitLoc, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.8f);
                            
                            // ПОДЖИГАЕМ БЛОКИ (5x5)
                            for (int x = -2; x <= 2; x++) {
                                for (int z = -2; z <= 2; z++) {
                                    if (Math.sqrt(x*x + z*z) <= 2.5) {
                                        Location fireLoc = hitLoc.clone().add(x, 0, z);
                                        if (fireLoc.getBlock().getType() == Material.AIR) {
                                            fireLoc.getBlock().setType(Material.FIRE);
                                        }
                                    }
                                }
                            }
                            
                            // СПАВНИМ 4 ВИЗЕР-СКЕЛЕТОВ
                            for (int i = 0; i < 4; i++) {
                                Location skeletonLoc = hitLoc.clone().add(
                                    (Math.random() - 0.5) * 4,
                                    0,
                                    (Math.random() - 0.5) * 4
                                );
                                
                                WitherSkeleton wither = world.spawn(skeletonLoc, WitherSkeleton.class);
                                wither.setTarget(null);
                                wither.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(30);
                                wither.setHealth(30);
                                wither.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                                
                                meteorOwners.put(wither.getUniqueId(), player.getUniqueId());
                            }
                            
                            meteor.remove();
                        }
                        
                        cooldowns.put(player.getUniqueId(), now);
                        charging.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }
                    
                    // ЧАСТИЦЫ ДЛЯ УВЕЛИЧЕНИЯ МЕТЕОРИТА
                    for (int i = 0; i < 20; i++) {
                        double offsetX = (Math.random() - 0.5) * 4;
                        double offsetY = (Math.random() - 0.5) * 4;
                        double offsetZ = (Math.random() - 0.5) * 4;
                        
                        Location particleLoc = meteor.getLocation().clone().add(offsetX, offsetY, offsetZ);
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                    }
                    
                    // ХВОСТ
                    for (int i = 0; i < 8; i++) {
                        Location trailLoc = meteor.getLocation().clone().subtract(0, i * 0.7, 0);
                        world.spawnParticle(Particle.FLAME, trailLoc, 5, 0.8, 0.8, 0.8, 0.02);
                        world.spawnParticle(Particle.LAVA, trailLoc, 3, 0.6, 0.6, 0.6, 0.01);
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, trailLoc, 8, 1.2, 1.2, 1.2, 0.01);
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