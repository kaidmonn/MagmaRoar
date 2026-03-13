package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isVillagerStaff(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cПосох жителя перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Получаем точку взгляда игрока
            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);
            
            // Звук активации (маяк)
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§aПосох жителя заряжается... 2 секунды до взрыва!");
            
            // Отмечаем место частицами
            player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 50, 1, 1, 1, 0.1);
            
            // СОЗДАЕМ ТОЛСТЫЙ ЛУЧ (от неба до земли)
            Location topLoc = targetLoc.clone().add(0, 30, 0);
            Location bottomLoc = targetLoc.clone().subtract(0, 5, 0);
            
            // Рисуем луч частицами
            new BukkitRunnable() {
                double t = 0;
                @Override
                public void run() {
                    if (t >= 40) { // 2 секунды (20 тиков = 1 сек)
                        this.cancel();
                        return;
                    }
                    
                    // Толстый луч из END_ROD частиц
                    for (int i = 0; i < 10; i++) {
                        double offsetX = (Math.random() - 0.5) * 2;
                        double offsetZ = (Math.random() - 0.5) * 2;
                        Location beamLoc = targetLoc.clone().add(offsetX, 15 + Math.sin(t * 0.5) * 5, offsetZ);
                        player.getWorld().spawnParticle(Particle.END_ROD, beamLoc, 5, 1, 5, 1, 0.02);
                    }
                    
                    // SONIC_BOOM частицы для эффектности
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLoc.clone().add(0, 15, 0), 3, 1, 10, 1, 0);
                    
                    t++;
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    World world = player.getWorld();
                    
                    // ЭПИЧНЫЕ ЧАСТИЦЫ ВЗРЫВА
                    world.spawnParticle(Particle.EXPLOSION, targetLoc, 5, 3, 2, 3, 0);
                    world.spawnParticle(Particle.FLASH, targetLoc, 10, 2, 1, 2, 0);
                    world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 50, 4, 3, 4, 0);
                    world.spawnParticle(Particle.END_ROD, targetLoc, 200, 5, 3, 5, 0.2);
                    world.spawnParticle(Particle.LAVA, targetLoc, 100, 3, 2, 3, 0.1);
                    
                    // Звук взрыва
                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                    
                    // УРОН ПО ИГРОКАМ (ТОЛЬКО ПО ГОРИЗОНТАЛИ, ВЫСОТА НЕОГРАНИЧЕНА)
                    int playersHit = 0;
                    for (Entity entity : world.getNearbyEntities(targetLoc, 5, 256, 5)) { // 256 блоков вверх/вниз
                        if (entity instanceof Player && !entity.equals(player)) {
                            Player target = (Player) entity;
                            // Урон 30 сердец (60 HP) - гарантированная смерть
                            target.damage(60, player);
                            playersHit++;
                            
                            // Эффекты на пораженных игроках
                            target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 20, 1, 1, 1, 0);
                            target.sendMessage("§c§lВАС ПОРАЗИЛ ПОСОХ ЖИТЕЛЯ!");
                            
                            // Подбрасываем вверх
                            target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 1.5, 0)));
                        }
                    }
                    
                    player.sendMessage("§a§lМОЩНЫЙ ВЗРЫВ! Уровень 20. Задето игроков: " + playersHit);
                    
                    // Ставим кулдаун
                    cooldowns.put(player.getUniqueId(), now);
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 40L); // 2 секунды
            
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