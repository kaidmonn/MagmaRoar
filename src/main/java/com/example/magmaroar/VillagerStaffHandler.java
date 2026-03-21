package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.NamespacedKey; // Добавлено
import org.bukkit.persistence.PersistentDataType; // Добавлено
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerStaffHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> charging = new HashMap<>();
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private static final int EXPLOSION_POWER = 20;

    // Ключ должен совпадать с тем, что ты указываешь при создании предмета
    private static final NamespacedKey STAFF_KEY = new NamespacedKey("magmaroar", "villager_staff");

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Теперь эта проверка работает мгновенно и надежно
        if (!isVillagerStaff(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if (charging.getOrDefault(player.getUniqueId(), false)) {
                player.sendMessage("§cПосох уже заряжается!");
                event.setCancelled(true);
                return;
            }

            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());

            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cПосох жителя перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Находим точку взрыва (куда смотрит игрок)
            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§aПосох жителя заряжается... 1.5 секунды до взрыва уровня 20!");

            player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 50, 1, 1, 1, 0.1);

            charging.put(player.getUniqueId(), true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        charging.remove(player.getUniqueId());
                        return;
                    }

                    World world = player.getWorld();
                    world.createExplosion(targetLoc, EXPLOSION_POWER, false, false, player);

                    // Эпичные эффекты
                    world.spawnParticle(Particle.EXPLOSION, targetLoc, 10, 5, 3, 5, 0);
                    world.spawnParticle(Particle.FLASH, targetLoc, 20, 3, 2, 3, 0);
                    world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 100, 6, 4, 6, 0);
                    world.spawnParticle(Particle.END_ROD, targetLoc, 300, 7, 5, 7, 0.2);
                    world.spawnParticle(Particle.LAVA, targetLoc, 150, 5, 3, 5, 0.1);

                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                    player.sendMessage("§a§lВЗРЫВ УРОВНЯ 20!");

                    charging.remove(player.getUniqueId());
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 30L);

            event.setCancelled(true);
        }
    }

    private boolean isVillagerStaff(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        // Проверяем наличие невидимой метки, которую мы дали предмету в классе VillagerStaffItem
        return item.getItemMeta().getPersistentDataContainer().has(STAFF_KEY, PersistentDataType.BYTE);
    }
}