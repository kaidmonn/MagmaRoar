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

    private final Map<UUID, Long> throwCooldowns = new HashMap<>();
    private final Map<UUID, Long> lastSwingTime = new HashMap<>();
    private final Map<UUID, ItemStack> thrownWeapons = new HashMap<>();
    
    private static final long THROW_COOLDOWN = 20 * 1000;
    private static final long FULL_SWING_TIME = 500;

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMjolnir(item)) return;

        long now = System.currentTimeMillis();
        Long lastSwing = lastSwingTime.get(player.getUniqueId());
        
        event.setCancelled(true);
        
        if (lastSwing != null && now - lastSwing >= FULL_SWING_TIME) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                
                // ОДНА молния с x8 уроном
                target.getWorld().strikeLightning(target.getLocation());
                
                player.sendMessage("§b⚡ БАБАХ! ⚡");
            }
        }
        
        lastSwingTime.put(player.getUniqueId(), now);
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
                player.sendMessage("§cЕщё " + secondsLeft + " сек");
                event.setCancelled(true);
                return;
            }
            
            ItemStack thrownItem = item.clone();
            player.getInventory().setItemInMainHand(null);
            
            World world = player.getWorld();
            Snowball projectile = world.spawn(player.getEyeLocation(), Snowball.class);
            projectile.setVelocity(player.getLocation().getDirection().multiply(2));
            projectile.setShooter(player);
            projectile.setCustomName("mjolnir");
            
            thrownWeapons.put(projectile.getUniqueId(), thrownItem);
            throwCooldowns.put(player.getUniqueId(), now);
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        if (!"mjolnir".equals(snowball.getCustomName())) return;
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player player = (Player) snowball.getShooter();
        
        // ОДНА молния в месте попадания
        snowball.getWorld().strikeLightning(snowball.getLocation());
        
        // Вернуть молот
        ItemStack item = thrownWeapons.remove(snowball.getUniqueId());
        if (item != null) {
            player.getInventory().addItem(item);
        }
        
        snowball.remove();
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Мьёльнир");
    }
}