package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    // Строковые ID для модели
    private static final String MODEL_SWORD = "1001";
    private static final String MODEL_TRIDENT = "1002";
    private static final String MODEL_MACE = "1003";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (!isBloodWeapon(item)) return;  // ← ИСПРАВЛЕНО!

        // Shift+ПКМ - переключение режима
        if (player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            int currentMode = weaponMode.getOrDefault(player.getUniqueId(), 0);
            int newMode = (currentMode + 1) % 3;
            weaponMode.put(player.getUniqueId(), newMode);
            
            switch (newMode) {
                case 0: // МЕЧ
                    updateItemWithModel(player, Material.NETHERITE_SWORD, MODEL_SWORD, "Кровавый меч", "red");
                    player.sendMessage("§cРежим: Кровавый меч");
                    break;
                case 1: // ТРЕЗУБЕЦ
                    updateItemWithModel(player, Material.TRIDENT, MODEL_TRIDENT, "Кровавый трезубец", "dark_aqua");
                    player.sendMessage("§3Режим: Кровавый трезубец");
                    break;
                case 2: // БУЛАВА
                    updateItemWithModel(player, Material.MACE, MODEL_MACE, "Кровавая булава", "dark_purple");
                    player.sendMessage("§5Режим: Кровавая булава");
                    break;
            }
            
            event.setCancelled(true);
            return;
        }

        // Логика броска
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
                
                // Создаём трезубец
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                // Сохраняем информацию для возврата
                ItemStack sourceItem = item.clone();
                thrownTridentSource.put(trident.getUniqueId(), sourceItem);
                
                lastThrowTime.put(player.getUniqueId(), now);
                
                // Удаляем трезубец из руки
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                event.setCancelled(true);
            }
        }
    }

    private void updateItemWithModel(Player player, Material material, String modelId, String displayName, String color) {
        // Удаляем текущий предмет из руки
        player.getInventory().setItemInMainHand(null);
        
        // Создаём команду с кавычками
        String command = "give " + player.getName() + " minecraft:" + material.name().toLowerCase() + "[" +
            "custom_model_data={strings:[\"" + modelId + "\"]}," +
            "item_name='{\"text\":\"" + displayName + "\",\"color\":\"" + color + "\",\"bold\":true}'" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;
        
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
            
            // Эффекты
            shooter.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            shooter.getWorld().spawnParticle(Particle.ASH, target.getLocation(), 20, 0.5, 0.5, 0.5, 0);
            shooter.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            
            shooter.sendMessage("§cЦель притянута!");
        }
        
        // ВСЕГДА ВОЗВРАЩАЕМ МЕЧ
        ItemStack returnItem = thrownTridentSource.remove(trident.getUniqueId());
        if (returnItem != null) {
            String command = "give " + shooter.getName() + " minecraft:netherite_sword[" +
                "custom_model_data={strings:[\"1001\"]}," +
                "item_name='{\"text\":\"Кровавый меч\",\"color\":\"red\",\"bold\":true}'," +
                "lore=['{\"text\":\"Урон: 14\",\"color\":\"gray\"}'," +
                      "'{\"text\":\"Shift+ПКМ: переключение режима\",\"color\":\"gray\"}'," +
                      "'{\"text\":\"Режимы: Меч → Трезубец → Булава\",\"color\":\"gray\"}']" +
                "] 1";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            shooter.sendMessage("§aКровавый меч вернулся!");
        }
        
        trident.remove();
    }

    // ← ИСПРАВЛЕННАЯ ПРОВЕРКА!
    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Кровавый");
    }
}