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
            
            // Сохраняем копию предмета
            ItemStack thrownItem = item.clone();
            
            // Удаляем предмет из руки
            player.getInventory().setItemInMainHand(null);
            
            World world = player.getWorld();
            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            
            // Используем снежок (он лучше летит)
            Snowball projectile = world.spawn(eyeLoc, Snowball.class);
            projectile.setVelocity(direction.multiply(2.5));
            projectile.setGlowing(true);
            projectile.setShooter(player);
            
            // Сохраняем связь
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
        
        // ГРОМКИЙ ЗВУК МОЛНИИ
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
        
        // ВИЗУАЛ МОЛНИИ (без разрушения блоков)
        world.strikeLightningEffect(hitLoc);
        
        // УРОН 3 СЕРДЦА (6 HP) + ЧАСТИЦЫ
        for (Entity e : world.getNearbyEntities(hitLoc, 4, 4, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                
                // Истинный урон (игнорирует броню)
                target.damage(6, player);
                
                // Частицы молнии на цели
                target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                    target.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                
                // Поджигаем цель? (для эффектности)
                target.setFireTicks(40); // 2 секунды огня
            }
        }
        
        // Эффекты взрыва
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 100, 3, 2, 3, 0.2);
        world.spawnParticle(Particle.FLASH, hitLoc, 20, 2, 2, 2, 0);
        world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 30, 2, 1, 2, 0);
        
        // ВОЗВРАТ МОЛОТА
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