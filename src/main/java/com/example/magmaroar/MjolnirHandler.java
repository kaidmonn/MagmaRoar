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

    private final Map<UUID, Long> throwCooldowns = new HashMap<>(); // КД на бросок
    private final Map<UUID, Long> lightningCooldowns = new HashMap<>(); // КД на молнию
    private final Map<UUID, ItemStack> thrownWeapons = new HashMap<>();
    
    private static final long THROW_COOLDOWN = 20 * 1000; // 20 секунд на бросок
    private static final long LIGHTNING_COOLDOWN = 2 * 1000; // 2 секунды на молнию
    private static final double MELEE_DAMAGE = 5.0; // 2.5 сердца

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isMjolnir(item)) return;
        
        // Устанавливаем урон 2.5 сердца
        event.setDamage(MELEE_DAMAGE);
        
        // Проверяем КД на молнию
        long now = System.currentTimeMillis();
        Long lastLightning = lightningCooldowns.get(player.getUniqueId());
        
        if (lastLightning == null || now - lastLightning >= LIGHTNING_COOLDOWN) {
            // Бьём молнией
            Location targetLoc = event.getEntity().getLocation();
            player.getWorld().strikeLightningEffect(targetLoc);
            player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.0f);
            
            // Частицы
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                targetLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            
            // Обновляем КД
            lightningCooldowns.put(player.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isMjolnir(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastThrow = throwCooldowns.get(player.getUniqueId());
            
            if (lastThrow != null && now - lastThrow < THROW_COOLDOWN) {
                long secondsLeft = (THROW_COOLDOWN - (now - lastThrow)) / 1000;
                player.sendMessage("§cБросок перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Сохраняем копию предмета
            ItemStack thrownItem = item.clone();
            
            // Удаляем предмет из руки
            player.getInventory().setItemInMainHand(null);
            
            World world = player.getWorld();
            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            
            // Снежок как снаряд
            Snowball projectile = world.spawn(eyeLoc, Snowball.class);
            projectile.setVelocity(direction.multiply(2.5));
            projectile.setGlowing(true);
            projectile.setShooter(player);
            
            // Сохраняем связь
            thrownWeapons.put(projectile.getUniqueId(), thrownItem);
            
            throwCooldowns.put(player.getUniqueId(), now);
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
        
        // Молния (гарантированная)
        world.strikeLightningEffect(hitLoc);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        
        // Урон 3 сердца по области (через setHealth)
        for (Entity e : world.getNearbyEntities(hitLoc, 4, 2, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                double newHealth = target.getHealth() - 6.0; // 3 сердца
                
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
        
        // Визуальные эффекты
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 50, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.FLASH, hitLoc, 10, 1, 1, 1, 0);
        
        // Возврат молота
        ItemStack returningItem = thrownWeapons.remove(projectileId);
        if (returningItem != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returningItem);
            if (!leftover.isEmpty()) {
                world.dropItemNaturally(player.getLocation(), returningItem);
            }
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