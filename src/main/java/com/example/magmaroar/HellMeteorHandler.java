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
    private final Set<UUID> charging = new HashSet<>(); // Для защиты от спама
    
    private static final long COOLDOWN = 60 * 1000; // 60 секунд
    private static final int METEOR_HEIGHT = 20;
    private static final float EXPLOSION_POWER = 10.0f;
    private static final int METEOR_SCALE = 6;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isHellMeteor(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Защита от спама
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
            World world = player.getWorld();
            
            // Звук запуска
            world.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.5f);
            player.sendMessage("§cАдский метеорит падает! (2 секунды)");
            
            charging.add(player.getUniqueId());

            // Запускаем падение
            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 40) { // 2 секунды
                        
                        // ВЗРЫВ (без разрушения блоков)
                        world.createExplosion(targetLoc, EXPLOSION_POWER, false, true, player);
                        
                        // Эффекты взрыва
                        world.spawnParticle(Particle.EXPLOSION, targetLoc, 30, 3, 3, 3, 0);
                        world.spawnParticle(Particle.FLASH, targetLoc, 20, 3, 3, 3, 0);
                        world.spawnParticle(Particle.LAVA, targetLoc, 300, 5, 4, 5, 0.1);
                        world.spawnParticle(Particle.FLAME, targetLoc, 400, 6, 5, 6, 0.05);
                        
                        world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                        world.playSound(targetLoc, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.8f);
                        
                        // Поджигаем блоки (только воздух)
                        for (int x = -3; x <= 3; x++) {
                            for (int z = -3; z <= 3; z++) {
                                if (Math.sqrt(x*x + z*z) <= 3.5) {
                                    Location fireLoc = targetLoc.clone().add(x, 0, z);
                                    if (fireLoc.getBlock().getType() == Material.AIR) {
                                        fireLoc.getBlock().setType(Material.FIRE);
                                    }
                                }
                            }
                        }
                        
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
                        charging.remove(player.getUniqueId());
                        
                        this.cancel();
                        return;
                    }
                    
                    // ВИЗУАЛ МЕТЕОРИТА (огненный шар x6)
                    double progress = (double) ticks / 40.0;
                    double height = METEOR_HEIGHT * (1 - progress);
                    
                    // Основная позиция метеорита
                    Location meteorLoc = targetLoc.clone().add(0, height, 0);
                    
                    // ОГНЕННЫЙ ШАР (визуально увеличенный)
                    for (int i = 0; i < 20; i++) {
                        double offsetX = (Math.random() - 0.5) * METEOR_SCALE;
                        double offsetY = (Math.random() - 0.5) * METEOR_SCALE;
                        double offsetZ = (Math.random() - 0.5) * METEOR_SCALE;
                        
                        Location particleLoc = meteorLoc.clone().add(offsetX, offsetY, offsetZ);
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                    }
                    
                    // ХВОСТ МЕТЕОРИТА
                    for (int i = 0; i < 10; i++) {
                        double trailOffset = i * 0.5;
                        Location trailLoc = meteorLoc.clone().subtract(0, trailOffset, 0);
                        
                        world.spawnParticle(Particle.FLAME, trailLoc, 5, 1, 1, 1, 0.02);
                        world.spawnParticle(Particle.LAVA, trailLoc, 3, 0.8, 0.8, 0.8, 0.01);
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, trailLoc, 10, 1.5, 1.5, 1.5, 0.01);
                    }
                    
                    // Звук падения
                    if (ticks % 10 == 0) {
                        world.playSound(meteorLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.8f);
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