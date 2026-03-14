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
    
    private static final long COOLDOWN = 60 * 1000;
    private static final int METEOR_HEIGHT = 20;
    private static final float EXPLOSION_POWER = 10.0f;

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

            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);
            Location spawnLoc = targetLoc.clone().add(0, METEOR_HEIGHT, 0);
            
            World world = player.getWorld();
            
            world.playSound(spawnLoc, Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.5f);
            
            // Просто частицы для визуала (без实体)
            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 40) {
                        
                        // ВЗРЫВ
                        world.createExplosion(targetLoc, EXPLOSION_POWER, false, true, player);
                        
                        // Визуальные эффекты
                        world.spawnParticle(Particle.EXPLOSION, targetLoc, 30, 3, 3, 3, 0);
                        world.spawnParticle(Particle.LAVA, targetLoc, 200, 5, 3, 5, 0.1);
                        world.spawnParticle(Particle.FLAME, targetLoc, 300, 5, 4, 5, 0.05);
                        
                        world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                        
                        // СПАВНИМ 4 ВИЗЕР-СКЕЛЕТОВ
                        for (int i = 0; i < 4; i++) {
                            Location skeletonLoc = targetLoc.clone().add(
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
                        
                        cooldowns.put(player.getUniqueId(), now);
                        player.sendMessage("§cМетеорит упал! Призваны визер-скелеты.");
                        
                        this.cancel();
                    }
                    
                    // Частицы падающего метеорита
                    Location currentLoc = targetLoc.clone().add(0, METEOR_HEIGHT - (ticks * 0.5), 0);
                    world.spawnParticle(Particle.FLAME, currentLoc, 30, 2, 2, 2, 0.02);
                    world.spawnParticle(Particle.LAVA, currentLoc, 15, 1, 1, 1, 0.01);
                    world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, currentLoc, 10, 1, 1, 1, 0.01);
                    
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