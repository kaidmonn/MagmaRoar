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

    private final Map<UUID, Integer> weaponMode = new HashMap<>(); 
    private final Map<UUID, Long> lastThrowTime = new HashMap<>();
    private final Map<UUID, ItemStack> thrownTridentSource = new HashMap<>();
    private static final long THROW_COOLDOWN = 10 * 1000;

    // Константы ID моделей (должны совпадать с BloodSwordItem)
    private static final int MODEL_SWORD = 1001;
    private static final int MODEL_TRIDENT = 1002;
    private static final int MODEL_MACE = 1003;

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
            
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            switch (newMode) {
                case 0: // МЕЧ
                    item.setType(Material.NETHERITE_SWORD);
                    meta.setCustomModelData(MODEL_SWORD);
                    player.sendMessage("§cРежим: Кровавый меч");
                    break;
                case 1: // ТРЕЗУБЕЦ
                    item.setType(Material.TRIDENT);
                    meta.setCustomModelData(MODEL_TRIDENT);
                    player.sendMessage("§3Режим: Кровавый трезубец");
                    break;
                case 2: // БУЛАВА
                    item.setType(Material.MACE);
                    meta.setCustomModelData(MODEL_MACE);
                    player.sendMessage("§5Режим: Кровавая булава");
                    break;
            }
            
            item.setItemMeta(meta);
            event.setCancelled(true);
            return;
        }

        // Логика броска (ПКМ без шифта в режиме трезубца)
        if (!player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            if (item.getType() == Material.TRIDENT) {
                long now = System.currentTimeMillis();
                Long lastThrow = lastThrowTime.get(player.getUniqueId());
                
                if (lastThrow != null && now - lastThrow < THROW_COOLDOWN) {
                    long secondsLeft = (THROW_COOLDOWN - (now - lastThrow)) / 1000;
                    player.sendMessage("§cБросок перезаряжается! Осталось: " + secondsLeft + " сек.");
                    event.setCancelled(true);
                    return;
                }
                
                // Создаем "возвратный" предмет (меч), чтобы он вернулся в инвентарь
                ItemStack sourceItem = item.clone();
                sourceItem.setType(Material.NETHERITE_SWORD);
                ItemMeta sourceMeta = sourceItem.getItemMeta();
                if (sourceMeta != null) {
                    sourceMeta.setCustomModelData(MODEL_SWORD);
                    sourceItem.setItemMeta(sourceMeta);
                }
                
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                thrownTridentSource.put(trident.getUniqueId(), sourceItem);
                lastThrowTime.put(player.getUniqueId(), now);
                
                // Убираем предмет из руки
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;
        
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
            // Эффекты...
            shooter.sendMessage("§cЦель притянута!");
        }
        
        ItemStack returnItem = thrownTridentSource.remove(trident.getUniqueId());
        if (returnItem != null) {
            if (!shooter.getInventory().addItem(returnItem).isEmpty()) {
                shooter.getWorld().dropItemNaturally(shooter.getLocation(), returnItem);
            }
        }
        trident.remove();
    }

    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Более надежная проверка через CustomModelData
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        int cmd = meta.getCustomModelData();
        return cmd == MODEL_SWORD || cmd == MODEL_TRIDENT || cmd == MODEL_MACE;
    }
}