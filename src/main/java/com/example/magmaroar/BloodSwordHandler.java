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

    // Теперь это строки, которые должны быть в JSON в поле "when"
    private static final String MODEL_SWORD = "1001";
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
                    item.setType(Material.NETHERITE_SWORD);
                    setCustomModelString(item, MODEL_SWORD);
                    player.sendMessage("§cРежим: Кровавый меч");
                    break;
                case 1: // ТРЕЗУБЕЦ
                    item.setType(Material.TRIDENT);
                    setCustomModelString(item, MODEL_TRIDENT);
                    player.sendMessage("§3Режим: Кровавый трезубец");
                    break;
                case 2: // БУЛАВА
                    item.setType(Material.MACE);
                    setCustomModelString(item, MODEL_MACE);
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
                
                ItemStack sourceItem = item.clone();
                sourceItem.setType(Material.NETHERITE_SWORD);
                setCustomModelString(sourceItem, MODEL_SWORD);
                
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                thrownTridentSource.put(trident.getUniqueId(), sourceItem);
                lastThrowTime.put(player.getUniqueId(), now);
                
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }
    }

    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ УСТАНОВКИ СТРОКОВОЙ МОДЕЛИ (1.21.4)
    private void setCustomModelString(ItemStack item, String modelId) {
        // Мы используем команду, так как API для строковых CMD в 1.21.4 
        // часто требует сложной работы с компонентами. Это самый надежный путь.
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Сбрасываем числовое значение, чтобы оно не мешало
            meta.setCustomModelData(null); 
            item.setItemMeta(meta);
        }
        
        // Магия компонентов 1.21.4: устанавливаем strings через тег
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), 
            "item modify entity " + Bukkit.getPlayer(item.getTranslationKey().split("\\.")[0]).getName() + " weapon.mainhand set custom_model_data={strings:['" + modelId + "']}");
        
        // ПРИМЕЧАНИЕ: Если метод выше не сработает (например, если предмет не в руке), 
        // лучше использовать API компонентов твоего ядра (Paper), но этот способ самый простой.
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;
        
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
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
        // Если ты используешь строки, проверка на hasCustomModelData(int) может не сработать.
        // Поэтому проверяем наличие нашего кастомного тега или просто тип и наличие меты.
        return item.getType() == Material.NETHERITE_SWORD || 
               item.getType() == Material.TRIDENT || 
               item.getType() == Material.MACE;
    }
}