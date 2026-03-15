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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class StormBladeHandler implements Listener {

    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final Random random = new Random();
    
    private static final long ABILITY_COOLDOWN = 30 * 1000; // 30 секунд
    private static final double PASSIVE_CHANCE = 0.15; // 15% шанс
    private static final double WEAPON_DAMAGE = 14.0;
    private static final float EXPLOSION_POWER = 4.0f;
    private static final double PROJECTILE_SPREAD = 0.05;
    private static final int PROJECTILE_COUNT = 7;

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isStormBlade(item)) return;

        // Устанавливаем урон 14
        event.setDamage(WEAPON_DAMAGE);

        // Проверяем пассивный шанс
        if (random.nextDouble() < PASSIVE_CHANCE) {
            
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                
                World world = target.getWorld();
                Location targetLoc = target.getLocation();
                
                // 1 ПОРЫВ ВЕТРА ПОД ЦЕЛЬЮ
                WindCharge windCharge = world.spawn(targetLoc, WindCharge.class);
                windCharge.setVelocity(new Vector(0, 0.8, 0)); // Умеренная сила
                windCharge.setShooter(player);
                windCharge.setYield(2.0f);
                
                player.sendMessage("§b§lШТОРМ! Цель подброшена порывом ветра!");
                
                // Молния сразу
                world.strikeLightningEffect(targetLoc);
                world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                
                // Урон от молнии
                target.damage(5.0, player);
                
                // Визуальные эффекты
                world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 10, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isStormBlade(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = abilityCooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < ABILITY_COOLDOWN) {
                long secondsLeft = (ABILITY_COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cСпособность перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            World world = player.getWorld();

            player.sendMessage("§b§lКЛИНОК БУРИ! 7 молний! (кулдаун 30 сек)");
            
            world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);

            for (int i = 0; i < PROJECTILE_COUNT; i++) {
                double spreadX = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                double spreadY = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                double spreadZ = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                
                Vector shotDirection = direction.clone().add(new Vector(spreadX, spreadY, spreadZ)).normalize();
                
                Snowball projectile = world.spawn(eyeLoc, Snowball.class);
                projectile.setVelocity(shotDirection.multiply(2.5));
                projectile.setGlowing(true);
                projectile.setShooter(player);
                projectile.setCustomName("§bМолния");
            }
            
            abilityCooldowns.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player player = (Player) snowball.getShooter();
        
        if (snowball.getCustomName() == null || !snowball.getCustomName().contains("Молния")) return;
        
        Location hitLoc = snowball.getLocation();
        World world = hitLoc.getWorld();
        
        world.strikeLightningEffect(hitLoc);
        world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, player);
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 30, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.FLASH, hitLoc, 5, 1, 1, 1, 0);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
        
        snowball.remove();
    }

    private boolean isStormBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Клинок бури");
    }
}