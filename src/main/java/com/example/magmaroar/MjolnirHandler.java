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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MjolnirHandler implements Listener {

    private final Map<UUID, Long> throwCooldowns = new HashMap<>();
    private final Map<UUID, Long> lastSwingTime = new HashMap<>();
    private final Map<UUID, ItemStack> thrownWeapons = new HashMap<>();
    
    private static final long THROW_COOLDOWN = 20 * 1000; // 20 секунд
    private static final long FULL_SWING_TIME = 500; // 0.5 секунды для полного замаха
    private static final double LIGHTNING_DAMAGE_MULTIPLIER = 8.0; // Урон x8

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMjolnir(item)) return;

        long now = System.currentTimeMillis();
        Long lastSwing = lastSwingTime.get(player.getUniqueId());
        
        // Отменяем обычный урон топора
        event.setCancelled(true);
        
        // Молния только при полном замахе
        if (lastSwing != null && now - lastSwing >= FULL_SWING_TIME) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                
                // Призываем молнию с увеличенным уроном
                strikeLightningWithDamage(target, player);
                
                player.sendMessage("§b§l⚡ МОЛНИЯ С УРОНОМ x8! ⚡");
            }
        }
        
        // Запоминаем время удара
        lastSwingTime.put(player.getUniqueId(), now);
    }

    private void strikeLightningWithDamage(LivingEntity target, Player owner) {
        World world = target.getWorld();
        Location loc = target.getLocation();
        
        // Призываем молнию (визуал)
        world.strikeLightningEffect(loc);
        
        // Звук
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        
        // Частицы
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0, 1, 0), 50, 1, 2, 1, 0.2);
        world.spawnParticle(Particle.FLASH, loc, 10, 1, 1, 1, 0);
        
        // УРОН x8 (обычная молния наносит 5♥, x8 = 40♥)
        target.damage(40.0, owner);
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
            projectile.setCustomName("§eМьёльнир");
            
            // Сохраняем связь
            thrownWeapons.put(projectile.getUniqueId(), thrownItem);
            
            throwCooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§eМьёльнир брошен!");
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
        
        // Визуал молнии
        world.strikeLightningEffect(hitLoc);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        
        // Частицы
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 50, 2, 2, 2, 0.2);
        world.spawnParticle(Particle.FLASH, hitLoc, 15, 2, 2, 2, 0);
        
        // Урон по области (x8)
        for (Entity e : world.getNearbyEntities(hitLoc, 4, 2, 4)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                target.damage(40.0, player);
            }
        }
        
        // Возврат молота
        ItemStack returningItem = thrownWeapons.remove(projectileId);
        if (returningItem != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returningItem);
            if (!leftover.isEmpty()) {
                world.dropItemNaturally(player.getLocation(), returningItem);
            }
            player.sendMessage("§e⚡ Мьёльнир вернулся! ⚡");
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