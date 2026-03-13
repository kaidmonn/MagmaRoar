package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrostSwordHandler implements Listener {

    private final Map<UUID, Integer> hitCounters = new HashMap<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>();
    private final Map<UUID, Location> frozenLocation = new HashMap<>();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isFrostSword(item)) return;

        // Проверяем, не заморожена ли цель
        if (isFrozen(target.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§bЦель заморожена!");
            return;
        }

        // Звук удара
        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
        
        // Замедление I на 3 секунды
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true, true));
        
        // Счётчик ударов
        UUID targetId = target.getUniqueId();
        int hits = hitCounters.getOrDefault(targetId, 0) + 1;
        
        if (hits >= 15) {
            // Заморозка на 4 секунды
            freezeTarget(target);
            hitCounters.remove(targetId);
            player.sendMessage("§bЦель полностью заморожена на 4 секунды!");
        } else {
            hitCounters.put(targetId, hits);
            player.sendMessage("§7Ударов до заморозки: " + hits + "/15");
        }
    }

    private void freezeTarget(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        
        // Запоминаем позицию заморозки
        Location center = target.getLocation();
        frozenLocation.put(targetId, center);
        frozenUntil.put(targetId, System.currentTimeMillis() + 4000); // 4 секунды
        
        // Создаём лёд ВОКРУГ цели (1х1х2, цель внутри)
        for (int y = 0; y < 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // Пропускаем центр (где стоит цель)
                    if (x == 0 && z == 0 && y == 0) continue;
                    if (x == 0 && z == 0 && y == 1) continue;
                    
                    Location iceLoc = center.clone().add(x, y, z);
                    if (iceLoc.getBlock().getType() == Material.AIR || 
                        iceLoc.getBlock().getType() == Material.WATER) {
                        iceLoc.getBlock().setType(Material.PACKED_ICE); // Упакованный лёд не тает в воду
                    }
                }
            }
        }
        
        // Эффекты заморозки
        target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation(), 50, 1, 1, 1, 0);
        target.getWorld().spawnParticle(org.bukkit.Particle.ITEM_SNOWBALL, target.getLocation(), 30, 0.5, 1, 0.5, 0);
        
        // Добавляем эффекты цели (полная остановка)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 254, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 128, false, false, false));
        target.setFreezeTicks(80); // Визуальная заморозка
        
        // Запускаем таймер на удаление льда
        new BukkitRunnable() {
            @Override
            public void run() {
                // Убираем лёд
                for (int y = 0; y < 2; y++) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            Location iceLoc = center.clone().add(x, y, z);
                            if (iceLoc.getBlock().getType() == Material.PACKED_ICE) {
                                iceLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }
                // Очищаем данные
                frozenUntil.remove(targetId);
                frozenLocation.remove(targetId);
                
                // Убираем эффекты с цели
                target.removePotionEffect(PotionEffectType.SLOWNESS);
                target.removePotionEffect(PotionEffectType.JUMP_BOOST);
                target.setFreezeTicks(0);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 80L); // 4 секунды
    }

    private boolean isFrozen(UUID targetId) {
        Long until = frozenUntil.get(targetId);
        return until != null && System.currentTimeMillis() < until;
    }

    private boolean isFrostSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Морозный меч");
    }
}