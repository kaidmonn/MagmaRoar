package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class BloodSwordHandler implements Listener {

    private final Map<UUID, Integer> weaponMode = new HashMap<>(); // 0-меч, 1-трезубец, 2-булава
    private final Map<UUID, Long> lastThrowTime = new HashMap<>();
    private static final long THROW_COOLDOWN = 10 * 1000; // 10 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isBloodWeapon(item)) return;

        // Shift+ПКМ - переключение режима
        if (player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            int currentMode = weaponMode.getOrDefault(player.getUniqueId(), 0);
            int newMode = (currentMode + 1) % 3;
            weaponMode.put(player.getUniqueId(), newMode);
            
            // Меняем предмет в зависимости от режима
            switch (newMode) {
                case 0:
                    item.setType(Material.NETHERITE_SWORD);
                    player.sendMessage("§cРежим: Кровавый меч");
                    break;
                case 1:
                    item.setType(Material.TRIDENT);
                    player.sendMessage("§3Режим: Кровавый трезубец");
                    break;
                case 2:
                    item.setType(Material.MACE);
                    player.sendMessage("§5Режим: Кровавая булава");
                    break;
            }
            event.setCancelled(true);
            return;
        }

        // ПКМ в режиме трезубца - бросок
        if (!player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            int currentMode = weaponMode.getOrDefault(player.getUniqueId(), 0);
            
            if (currentMode == 1 && item.getType() == Material.TRIDENT) {
                long now = System.currentTimeMillis();
                Long lastThrow = lastThrowTime.get(player.getUniqueId());
                
                if (lastThrow != null && now - lastThrow < THROW_COOLDOWN) {
                    long secondsLeft = (THROW_COOLDOWN - (now - lastThrow)) / 1000;
                    player.sendMessage("§cБросок перезаряжается! Осталось: " + secondsLeft + " сек.");
                    event.setCancelled(true);
                    return;
                }
                
                // Бросаем трезубец
                ThrownTrident trident = player.launchProjectile(ThrownTrident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(AbstractTrident.PickupStatus.DISALLOWED);
                
                lastThrowTime.put(player.getUniqueId(), now);
                player.sendMessage("§aКровавый трезубец брошен!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof ThrownTrident)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Player shooter = (Player) event.getEntity().getShooter();
        ThrownTrident trident = (ThrownTrident) event.getEntity();
        
        // Притягиваем цель к игроку
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            
            // Телепортируем цель к игроку
            target.teleport(shooter.getLocation().add(0, 1, 0));
            
            // Эффекты
            shooter.getWorld().spawnParticle(org.bukkit.Particle.ASH, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            shooter.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            
            shooter.sendMessage("§cЦель притянута!");
        }
        
        // Убираем трезубец
        trident.remove();
    }

    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Кровавый");
    }
}