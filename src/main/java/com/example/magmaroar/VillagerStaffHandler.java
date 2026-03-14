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
    private final Map<UUID, Boolean> charging = new HashMap<>();
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private static final int RADIUS_1 = 5;  // Ближняя зона
    private static final int RADIUS_2 = 10; // Дальняя зона
    private static final double DAMAGE_1 = 30.0; // 15 сердец (30 HP)
    private static final double DAMAGE_2 = 10.0; // 5 сердец (10 HP)

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

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

            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§aПосох жителя заряжается... 1.5 секунды до взрыва!");

            player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 50, 1, 1, 1, 0.1);

            charging.put(player.getUniqueId(), true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    World world = player.getWorld();

                    // Эффекты взрыва
                    world.spawnParticle(Particle.EXPLOSION, targetLoc, 5, 3, 2, 3, 0);
                    world.spawnParticle(Particle.FLASH, targetLoc, 10, 2, 1, 2, 0);
                    world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 50, 4, 3, 4, 0);
                    world.spawnParticle(Particle.END_ROD, targetLoc, 200, 5, 3, 5, 0.2);
                    world.spawnParticle(Particle.LAVA, targetLoc, 100, 3, 2, 3, 0.1);

                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

                    // УРОН ПО ЗОНАМ
                    int entitiesHit = 0;
                    int zone1 = 0;
                    int zone2 = 0;

                    for (org.bukkit.entity.Entity entity : world.getNearbyEntities(targetLoc, RADIUS_2, RADIUS_2, RADIUS_2)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            LivingEntity target = (LivingEntity) entity;
                            double distance = target.getLocation().distance(targetLoc);

                            // Определяем зону
                            if (distance <= RADIUS_1) {
                                // Ближняя зона - 15 сердец
                                target.setHealth(Math.max(0, target.getHealth() - DAMAGE_1));
                                zone1++;
                                if (target instanceof Player) {
                                    target.sendMessage("§c§lВЫ В ЭПИЦЕНТРЕ! -15 сердец");
                                }
                            } else if (distance <= RADIUS_2) {
                                // Дальняя зона - 5 сердец
                                target.setHealth(Math.max(0, target.getHealth() - DAMAGE_2));
                                zone2++;
                                if (target instanceof Player) {
                                    target.sendMessage("§cВас задело взрывом! -5 сердец");
                                }
                            }

                            entitiesHit++;

                            // Эффекты на цели
                            target.getWorld().spawnParticle(Particle.SONIC_BOOM, 
                                target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
                        }
                    }

                    player.sendMessage("§a§lМОЩНЫЙ ВЗРЫВ!");
                    player.sendMessage("§cБлижняя зона (до 5 блоков): " + zone1 + " целей (-15♥)");
                    player.sendMessage("§eДальняя зона (5-10 блоков): " + zone2 + " целей (-5♥)");
                    player.sendMessage("§7Всего поражено: " + entitiesHit + " существ");

                    charging.remove(player.getUniqueId());
                    cooldowns.put(player.getUniqueId(), now);
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 30L); // 1.5 секунды

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