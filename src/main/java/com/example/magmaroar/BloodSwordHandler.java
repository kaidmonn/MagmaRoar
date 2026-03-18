package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BloodSwordHandler implements Listener {

    private final Map<UUID, Integer> weaponMode = new HashMap<>(); // 0-меч, 1-трезубец, 2-булава
    private final Map<UUID, Long> lastThrowTime = new HashMap<>();
    private final Map<UUID, ItemStack> thrownTridentSource = new HashMap<>();
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
            
            // Сохраняем текущий custom_model_data
            int customModelData = 1;
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                customModelData = item.getItemMeta().getCustomModelData();
            }
            
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
            
            // Восстанавливаем custom_model_data после смены типа
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
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
                
                // Сохраняем исходный предмет с custom_model_data
                ItemStack sourceItem = item.clone();
                sourceItem.setType(Material.NETHERITE_SWORD);
                
                // Убеждаемся что custom_model_data сохранился
                if (sourceItem.hasItemMeta()) {
                    ItemMeta sourceMeta = sourceItem.getItemMeta();
                    if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                        sourceMeta.setCustomModelData(item.getItemMeta().getCustomModelData());
                        sourceItem.setItemMeta(sourceMeta);
                    }
                }
                
                // Бросаем трезубец
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.0));
                trident.setShooter(player);
                trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                thrownTridentSource.put(trident.getUniqueId(), sourceItem);
                lastThrowTime.put(player.getUniqueId(), now);
                player.sendMessage("§aКровавый трезубец брошен!");
                
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Trident trident = (Trident) event.getEntity();
        Player shooter = (Player) trident.getShooter();
        
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
            
            shooter.getWorld().spawnParticle(org.bukkit.Particle.ASH, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            shooter.getWorld().spawnParticle(org.bukkit.Particle.CRIMSON_SPORE, target.getLocation(), 20, 0.5, 0.5, 0.5, 0);
            shooter.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            
            shooter.sendMessage("§cЦель притянута!");
        }
        
        ItemStack sword = thrownTridentSource.remove(trident.getUniqueId());
        if (sword != null) {
            HashMap<Integer, ItemStack> leftover = shooter.getInventory().addItem(sword);
            if (!leftover.isEmpty()) {
                shooter.getWorld().dropItemNaturally(shooter.getLocation(), sword);
            }
        }
        
        trident.remove();
    }

    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Кровавый");
    }
}