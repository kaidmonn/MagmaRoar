package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShadowSwordHandler implements Listener {

    private final Map<UUID, Long> invisibilityUntil = new HashMap<>();
    private final Map<UUID, Integer> hitCounter = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private static final long EFFECT_DURATION = 30 * 1000; // 30 секунд
    private static final long COOLDOWN_DURATION = 60 * 1000; // 60 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isShadowSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Проверка кулдауна
            long now = System.currentTimeMillis();
            Long cooldown = cooldownUntil.get(player.getUniqueId());
            
            if (cooldown != null && now < cooldown) {
                long secondsLeft = (cooldown - now) / 1000;
                player.sendMessage("§cТеневой меч перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Активация невидимости
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 2, false, false, true)); // Speed III
            
            invisibilityUntil.put(player.getUniqueId(), now + EFFECT_DURATION);
            hitCounter.put(player.getUniqueId(), 0);
            
            // Скрываем броню (через метаданные)
            hideArmor(player, true);
            
            player.sendMessage("§8Вы стали невидимым на 30 секунд!");
            player.sendMessage("§7После 3 ударов эффект пропадёт.");
            
            event.setCancelled(true);
            
            // Запускаем таймер на окончание эффекта
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (invisibilityUntil.containsKey(player.getUniqueId())) {
                        removeEffects(player);
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 600L); // 30 секунд
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Проверяем, невидим ли игрок
        if (isInvisible(player.getUniqueId())) {
            int hits = hitCounter.getOrDefault(player.getUniqueId(), 0) + 1;
            
            if (hits >= 3) {
                // Снимаем эффекты после 3 ударов
                removeEffects(player);
                player.sendMessage("§cВас ударили 3 раза! Невидимость пропала.");
                
                // Устанавливаем кулдаун
                cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_DURATION);
            } else {
                hitCounter.put(player.getUniqueId(), hits);
                player.sendMessage("§7Осталось ударов до снятия невидимости: " + (3 - hits));
            }
        }
    }

    private void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
        hideArmor(player, false);
        
        invisibilityUntil.remove(player.getUniqueId());
        hitCounter.remove(player.getUniqueId());
        
        player.sendMessage("§8Эффект теневого меча закончился.");
    }

    private void hideArmor(Player player, boolean hide) {
        // Временно просто сообщение, позже можно добавить полноценное скрытие брони
        if (hide) {
            player.sendMessage("§7(Броня скрыта)");
        }
    }

    private boolean isInvisible(UUID playerId) {
        Long until = invisibilityUntil.get(playerId);
        return until != null && System.currentTimeMillis() < until;
    }

    private boolean isShadowSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Теневой меч");
    }
}