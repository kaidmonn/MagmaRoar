package com.example.magmaroar;

import org.bukkit.Bukkit;
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

    // Строковые ID для модели (должны совпадать с JSON)
    private static final String MODEL_SWORD = "1001.0";
    private static final String MODEL_TRIDENT = "1002.0";
    private static final String MODEL_MACE = "1003.0";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (!isBloodWeapon(item)) return;

        // Shift+ПКМ - переключение режима
        if (player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            int currentMode = weaponMode.getOrDefault(player.getUniqueId(), 0);
            int newMode = (currentMode + 1) % 3;
            weaponMode.put(player.getUniqueId(), newMode);
            
            switch (newMode) {
                case 0: // МЕЧ
                    updateItemWithModel(player, Material.NETHERITE_SWORD, MODEL_SWORD, "§cКровавый меч");
                    player.sendMessage("§cРежим: Кровавый меч");
                    break;
                case 1: // ТРЕЗУБЕЦ
                    updateItemWithModel(player, Material.TRIDENT, MODEL_TRIDENT, "§3Кровавый трезубец");
                    player.sendMessage("§3Режим: Кровавый трезубец");
                    break;
                case 2: // БУЛАВА
                    updateItemWithModel(player, Material.MACE, MODEL_MACE, "§5Кровавая булава");
                    player.sendMessage("§5Режим: Кровавая булава");
                    break;
            }
            
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
                
                // Сохраняем информацию для возврата
                String currentModel = getCurrentModel(item);
                
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                // Сохраняем данные для возврата
                thrownTridentSource.put(trident.getUniqueId(), 
                    createReturnItem(currentModel, player));
                
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

    private void updateItemWithModel(Player player, Material material, String modelId, String displayName) {
        // Удаляем текущий предмет из руки
        player.getInventory().setItemInMainHand(null);
        
        // Создаём команду для выдачи нового предмета с моделью
        String command = "give " + player.getName() + " minecraft:" + material.name().toLowerCase() + "[" +
            "custom_model_data={strings:[\"" + modelId + "\"]}," +
            "item_name='\"" + displayName + "\"'" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private ItemStack createReturnItem(String modelId, Player owner) {
        // Создаём предмет для возврата через команду
        String command = "give " + owner.getName() + " minecraft:netherite_sword[" +
            "custom_model_data={strings:[\"" + modelId + "\"]}," +
            "item_name='\"§cКровавый меч\"'" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        
        // Возвращаем null, так как предмет уже будет в инвентаре при возврате
        // Но для карты нам нужно что-то вернуть
        return new ItemStack(Material.NETHERITE_SWORD);
    }

    private String getCurrentModel(ItemStack item) {
        // По умолчанию возвращаем модель меча
        // В реальности нужно парсить предмет, но для простоты:
        if (item.getType() == Material.NETHERITE_SWORD) return MODEL_SWORD;
        if (item.getType() == Material.TRIDENT) return MODEL_TRIDENT;
        if (item.getType() == Material.MACE) return MODEL_MACE;
        return MODEL_SWORD;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;
        
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
            
            // Эффекты
            shooter.getWorld().spawnParticle(org.bukkit.Particle.ASH, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            shooter.getWorld().spawnParticle(org.bukkit.Particle.CRIMSON_SPORE, target.getLocation(), 20, 0.5, 0.5, 0.5, 0);
            shooter.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            
            shooter.sendMessage("§cЦель притянута!");
        }
        
        ItemStack returnItem = thrownTridentSource.remove(trident.getUniqueId());
        if (returnItem != null) {
            // Возвращаем меч через команду
            String currentModel = getCurrentModel(returnItem);
            String command = "give " + shooter.getName() + " minecraft:netherite_sword[" +
                "custom_model_data={strings:[\"" + currentModel + "\"]}," +
                "item_name='\"§cКровавый меч\"'" +
                "] 1";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        
        trident.remove();
    }

    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Проверяем по типу и наличию меты
        return item.getType() == Material.NETHERITE_SWORD || 
               item.getType() == Material.TRIDENT || 
               item.getType() == Material.MACE;
    }
}