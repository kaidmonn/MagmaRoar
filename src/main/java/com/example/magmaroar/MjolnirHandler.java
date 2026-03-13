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
            
            // Снежок как снаряд
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
        
        // МОЛНИЯ + ЗВУК
        world.strikeLightningEffect(hitLoc);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
        
        // УРОН 3 СЕРДЦА ПРИ БРОСКЕ
        for (Entity e : world.getNearbyEntities(hitLoc, 4, 4, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                target.damage(6, player); // 3 сердца
                target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                    target.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
            }
        }
        
        // Эффекты
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 100, 3, 2, 3, 0.2);
        world.spawnParticle(Particle.FLASH, hitLoc, 20, 2, 2, 2, 0);
        
        // ВОЗВРАТ
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

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isMjolnir(item)) return;
        
        // ОБЫЧНЫЙ УРОН 2.5 СЕРДЦА (5 HP)
        event.setDamage(5.0);
        
        // МОЛНИЯ ПРИ УДАРЕ (шанс 50% для эффектности)
        if (Math.random() < 0.5) {
            Location targetLoc = event.getEntity().getLocation();
            player.getWorld().strikeLightningEffect(targetLoc);
            player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
            
            // Частицы
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                targetLoc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        }
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Мьёльнир");
    }
}