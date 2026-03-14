 package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
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
    private final Map<UUID, Boolean> charging = new HashMap<>(); // Для защиты от мульти-клика
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private static final int DAMAGE_RADIUS = 8; // Увеличенный радиус (было 5)
    private static final double DAMAGE = 70.0; // Увеличенный урон (35 сердец)

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isVillagerStaff(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            // Защита от мульти-клика
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

            // Получаем точку взгляда
            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);

            // Звук активации
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§aПосох жителя заряжается... 1.5 секунды до взрыва!");

            // Отмечаем место частицами
            player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 50, 1, 1, 1, 0.1);

            // Блокируем повторный клик
            charging.put(player.getUniqueId(), true);

            // Задержка 1.5 секунды (30 тиков)
            new BukkitRunnable() {
                @Override
                public void run() {
                    World world = player.getWorld();

                    // Эпичные частицы
                    world.spawnParticle(Particle.EXPLOSION, targetLoc, 5, 3, 2, 3, 0);
                    world.spawnParticle(Particle.FLASH, targetLoc, 10, 2, 1, 2, 0);
                    world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 50, 4, 3, 4, 0);
                    world.spawnParticle(Particle.END_ROD, targetLoc, 200, 5, 3, 5, 0.2);
                    world.spawnParticle(Particle.LAVA, targetLoc, 100, 3, 2, 3, 0.1);

                    // Звук взрыва
                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

                    // УРОН ПО ВСЕМ В РАДИУСЕ
                    int entitiesHit = 0;
                    for (org.bukkit.entity.Entity entity : world.getNearbyEntities(targetLoc, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            LivingEntity target = (LivingEntity) entity;
                            target.setHealth(Math.max(0, target.getHealth() - DAMAGE));
                            entitiesHit++;

                            // Эффекты на цели
                            target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 20, 1, 1, 1, 0);

                            if (target instanceof Player) {
                                target.sendMessage("§c§lВАС ПОРАЗИЛ ПОСОХ ЖИТЕЛЯ!");
                            }
                        }
                    }

                    player.sendMessage("§a§lМОЩНЫЙ ВЗРЫВ! Радиус " + DAMAGE_RADIUS + " блоков. Задето существ: " + entitiesHit);

                    // Снимаем блокировку и ставим кулдаун
                    charging.remove(player.getUniqueId());
                    cooldowns.put(player.getUniqueId(), now);
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 30L); // 30 тиков = 1.5 секунды

            event.setCancelled(true);
        }
    }

    private boolean isVillagerStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Посох жителя");
    }
} 