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
    
    private static final long COOLDOWN = 30 * 1000;
    private static final int METEOR_HEIGHT = 20;
    private static final float EXPLOSION_POWER = 7.0f;
    private static final int EXPLOSION_RADIUS = 7;

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
            Location spawnLoc = targetLoc.clone().add(0, METEOR_HEIGHT, 0);
            
            World world = player.getWorld();
            
            world.playSound(spawnLoc, Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.5f);
            
            LargeFireball visualMeteor = world.spawn(spawnLoc, LargeFireball.class);
            visualMeteor.setVelocity(new Vector(0, -0.5, 0));
            visualMeteor.setYield(0);
            visualMeteor.setIsIncendiary(false);
            visualMeteor.setGlowing(true);
            
            visualMeteor.setCustomName("§c§lМЕТЕОРИТ");
            visualMeteor.setCustomNameVisible(true);
            
            meteorProjectiles.add(visualMeteor.getUniqueId());
            meteorOwners.put(visualMeteor.getUniqueId(), player.getUniqueId());
            
            cooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§cАдский метеорит падает! (2 секунды)");
            event.setCancelled(true);

            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (visualMeteor == null || visualMeteor.isDead() || ticks >= 40) {
                        
                        if (visualMeteor != null && !visualMeteor.isDead()) {
                            Location hitLoc = visualMeteor.getLocation();
                            
                            world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, player);
                            
                            world.spawnParticle(Particle.EXPLOSION_HUGE, hitLoc, 10, EXPLOSION_RADIUS, 2, EXPLOSION_RADIUS, 0);
                            world.spawnParticle(Particle.LAVA, hitLoc, 100, EXPLOSION_RADIUS, 2, EXPLOSION_RADIUS, 0.1);
                            world.spawnParticle(Particle.FLAME, hitLoc, 200, EXPLOSION_RADIUS, 3, EXPLOSION_RADIUS, 0.05);
                            
                            world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                            
                            for (int i = 0; i < 4; i++) {
                                Location spawnLoc = hitLoc.clone().add(
                                    (Math.random() - 0.5) * 4,
                                    0.5,
                                    (Math.random() - 0.5) * 4
                                );
                                
                                MagmaCube magma = world.spawn(spawnLoc, MagmaCube.class);
                                magma.setSize(3);
                                magma.setTarget(null);
                                magma.setAI(true);
                                magma.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20);
                                magma.setHealth(20);
                                magma.setRemoveWhenFarAway(false);
                                magma.setPersistent(true);
                                
                                meteorOwners.put(magma.getUniqueId(), player.getUniqueId());
                            }
                            
                            meteorProjectiles.remove(visualMeteor.getUniqueId());
                            meteorOwners.remove(visualMeteor.getUniqueId());
                            visualMeteor.remove();
                        }
                        
                        this.cancel();
                    }
                    
                    if (visualMeteor != null && !visualMeteor.isDead()) {
                        world.spawnParticle(Particle.FLAME, visualMeteor.getLocation(), 10, 1, 1, 1, 0.02);
                        world.spawnParticle(Particle.LAVA, visualMeteor.getLocation(), 5, 0.5, 0.5, 0.5, 0.01);
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof MagmaCube) {
            MagmaCube magma = (MagmaCube) event.getEntity();
            UUID ownerId = meteorOwners.get(magma.getUniqueId());
            
            if (ownerId != null && event.getTarget() instanceof Player && 
                ((Player) event.getTarget()).getUniqueId().equals(ownerId)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof MagmaCube) {
            MagmaCube magma = (MagmaCube) event.getEntity();
            if (meteorOwners.containsKey(magma.getUniqueId())) {
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isHellMeteor(ItemStack item) {
        if (item == null || item.getType() != Material.FIRE_CHARGE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Адский метеорит");
    }
}