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
import org.bukkit.util.Vector;

import java.util.*;

public class MjolnirHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> thrownWeapons = new HashMap<>();
    private static final long COOLDOWN = 20 * 1000; // 20 секунд
    private static final double MELEE_DAMAGE = 5.0; // 2.5 сердца
    private static final double THROW_DAMAGE = 6.0; // 3 сердца

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isMjolnir(item)) return;
        
        // Отменяем обычный урон
        event.setCancelled(true);
        
        // Прямой урон через setHealth
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            double newHealth = target.getHealth() - MELEE_DAMAGE;
            
            if (newHealth <= 0) {
                target.setHealth(0);
                target.damage(1);
            } else {
                target.setHealth(newHealth);
            }
            
            // Эффекты
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isMjolnir(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cМьёльнир перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            ItemStack thrownItem = item.clone();
            player.getInventory().setItemInMainHand(null);
            
            World world = player.getWorld();
            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            
            Snowball projectile = world.spawn(eyeLoc, Snowball.class);
            projectile.setVelocity(direction.multiply(2.5));
            projectile.setGlowing(true);
            projectile.setShooter(player);
            
            thrownWeapons.put(projectile.getUniqueId(), thrownItem);
            cooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§bМьёльнир брошен!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player player = (Player) snowball.getShooter();
        UUID projectileId = snowball.getUniqueId();
        
        if (!thrownWeapons.containsKey(projectileId)) return;
        
        Location hitLoc = snowball.getLocation();
        World world = hitLoc.getWorld();
        
        // Молния
        world.strikeLightningEffect(hitLoc);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        
        // Урон по области
        for (Entity e : world.getNearbyEntities(hitLoc, 4, 2, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                double newHealth = target.getHealth() - THROW_DAMAGE;
                
                if (newHealth <= 0) {
                    target.setHealth(0);
                    target.damage(1);
                } else {
                    target.setHealth(newHealth);
                }
                
                target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                    target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
        
        // Возврат
        ItemStack returningItem = thrownWeapons.remove(projectileId);
        if (returningItem != null) {
            player.getInventory().addItem(returningItem);
            player.sendMessage("§b⚡ Мьёльнир вернулся! ⚡");
        }
        
        snowball.remove();
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Мьёльнир");
    }
}