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
import org.bukkit.scheduler.BukkitRunnable;
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
            
            // Используем трезубец вместо снежка
            Trident trident = world.spawn(eyeLoc, Trident.class);
            trident.setVelocity(direction.multiply(2.0));
            trident.setShooter(player);
            trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
            trident.setGlowing(true);
            
            // Сохраняем связь
            thrownWeapons.put(trident.getUniqueId(), thrownItem);
            
            cooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§bМьёльнир брошен!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        
        Trident trident = (Trident) event.getEntity();
        if (!(trident.getShooter() instanceof Player)) return;
        
        Player player = (Player) trident.getShooter();
        UUID tridentId = trident.getUniqueId();
        
        if (!thrownWeapons.containsKey(tridentId)) return;
        
        Location hitLoc = trident.getLocation();
        World world = hitLoc.getWorld();
        
        // МОЛНИЯ (без разрушения блоков)
        world.strikeLightningEffect(hitLoc);
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        
        // УРОН 3 СЕРДЦА
        for (Entity e : world.getNearbyEntities(hitLoc, 3, 2, 3)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                LivingEntity target = (LivingEntity) e;
                target.damage(6, player);
                target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                    target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
            }
        }
        
        // Эффекты
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 50, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.FLASH, hitLoc, 10, 1, 1, 1, 0);
        
        // ВОЗВРАТ МОЛОТА
        ItemStack returningItem = thrownWeapons.remove(tridentId);
        if (returningItem != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returningItem);
            if (!leftover.isEmpty()) {
                world.dropItemNaturally(player.getLocation(), returningItem);
            }
            player.sendMessage("§bМьёльнир вернулся!");
        }
        
        trident.remove();
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Мьёльнир");
    }
}
