package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerStaffHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> charging = new HashMap<>();
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private static final int EXPLOSION_POWER = 20;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Проверяем предмет по скрытому ключу (не по имени!)
        if (!isVillagerStaff(item)) return;

        // Обработка правой кнопки мыши
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // 1. Проверка на текущую зарядку
            if (charging.getOrDefault(player.getUniqueId(), false)) {
                player.sendMessage("§cПосох уже заряжается!");
                event.setCancelled(true);
                return;
            }

            // 2. Проверка кулдауна
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cПосох перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // 3. Логика подготовки
            Location targetLoc = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§aПосох гудит... Взрыв уровня 20 через 1.5 секунды!");
            
            player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 50, 1, 1, 1, 0.1);
            charging.put(player.getUniqueId(), true);

            // 4. Таймер взрыва
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        charging.remove(player.getUniqueId());
                        return;
                    }

                    World world = player.getWorld();
                    // Создаем взрыв (сила 20, без огня, без разрушения блоков)
                    world.createExplosion(targetLoc, EXPLOSION_POWER, false, false, player);

                    // Эпичные эффекты (частицы)
                    world.spawnParticle(Particle.EXPLOSION, targetLoc, 10, 2, 2, 2, 0);
                    world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 50, 4, 4, 4, 0);
                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

                    player.sendMessage("§a§lБА-БАХ!");
                    
                    charging.remove(player.getUniqueId());
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }.runTaskLater(MagmaRoarPlugin.getPlugin(MagmaRoarPlugin.class), 30L); // 30 тиков = 1.5 сек

            event.setCancelled(true);
        }
    }

    private boolean isVillagerStaff(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Проверяем наличие нашего уникального ключа
        return item.getItemMeta().getPersistentDataContainer().has(VillagerStaffItem.STAFF_KEY, PersistentDataType.BYTE);
    }
}