package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
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

public class OrbitalCannonHandler implements Listener {

    private final Map<UUID, Long> lastUseTimeNormal = new HashMap<>();
    private final Map<UUID, Long> lastUseTimeRing = new HashMap<>();
    private static final long COOLDOWN_NORMAL = 25 * 1000; // 25 секунд
    private static final long COOLDOWN_RING = 3 * 60 * 1000; // 3 минуты

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isOrbitalCannon(item)) return;

        // ПКМ - обычный режим (5 мгновенных взрывов)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleNormalMode(player);
            event.setCancelled(true);
        }
        
        // ЛКМ - кольцевой режим (спавн ТНТ как в OSC)
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            handleRingMode(player);
            event.setCancelled(true);
        }
    }

    private void handleNormalMode(Player player) {
        long now = System.currentTimeMillis();
        Long lastUse = lastUseTimeNormal.get(player.getUniqueId());
        
        if (lastUse != null && now - lastUse < COOLDOWN_NORMAL) {
            long secondsLeft = (COOLDOWN_NORMAL - (now - lastUse)) / 1000;
            player.sendMessage("§cОбычный режим перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }
        
        Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);
        
        // Мгновенные взрывы (без спавна ТНТ)
        for (int i = 0; i < 5; i++) {
            player.getWorld().createExplosion(targetLoc, 4.0f, false, false, player);
        }
        
        player.sendMessage("§5Орбитальная пушка: 5 мгновенных взрывов!");
        lastUseTimeNormal.put(player.getUniqueId(), now);
    }

    private void handleRingMode(Player player) {
        long now = System.currentTimeMillis();
        Long lastUse = lastUseTimeRing.get(player.getUniqueId());
        
        if (lastUse != null && now - lastUse < COOLDOWN_RING) {
            long minutesLeft = ((COOLDOWN_RING - (now - lastUse)) / 1000) / 60;
            long secondsLeft = ((COOLDOWN_RING - (now - lastUse)) / 1000) % 60;
            player.sendMessage("§cКольцевой режим перезаряжается! Осталось: " + minutesLeft + " мин " + secondsLeft + " сек.");
            return;
        }
        
        Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 0, 0.5);
        World world = player.getWorld();
        
        // Параметры как в Orbital Strike Cannon [citation:1][citation:5]
        int rings = 5;
        double baseRadius = 3.0;
        double radiusIncrease = 4.0;
        double height = 25.0; // Высота спавна ТНТ
        int fuseTicks = 40; // 2 секунды до взрыва
        
        player.sendMessage("§5§lКОЛЬЦЕВОЙ РЕЖИМ! 100+ ТНТ ПАДАЕТ С НЕБА!");
        
        for (int ring = 0; ring < rings; ring++) {
            double radius = baseRadius + (ring * radiusIncrease);
            int tntCount = 20 + (ring * 8); // Больше ТНТ в дальних кольцах
            
            for (int i = 0; i < tntCount; i++) {
                double angle = 2 * Math.PI * i / tntCount;
                double x = targetLoc.getX() + radius * Math.cos(angle);
                double z = targetLoc.getZ() + radius * Math.sin(angle);
                
                // Спавним ТНТ высоко в небе
                Location tntLoc = new Location(world, x, targetLoc.getY() + height, z);
                TNTPrimed tnt = world.spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(fuseTicks + ring * 5); // Разная задержка
                tnt.setYield(4.0f);
                tnt.setIsIncendiary(false); // Не поджигает блоки
                tnt.setGlowing(true);
                
                // Добавляем небольшую случайность в падении
                tnt.setVelocity(new org.bukkit.util.Vector(
                    (Math.random() - 0.5) * 0.2,
                    -0.3,
                    (Math.random() - 0.5) * 0.2
                ));
            }
        }
        
        // Центральный мощный ТНТ
        Location centerLoc = new Location(world, targetLoc.getX(), targetLoc.getY() + height + 5, targetLoc.getZ());
        TNTPrimed centerTNT = world.spawn(centerLoc, TNTPrimed.class);
        centerTNT.setFuseTicks(fuseTicks + 10);
        centerTNT.setYield(6.0f);
        centerTNT.setIsIncendiary(false);
        centerTNT.setGlowing(true);
        
        // Запускаем таймер для создания дополнительных взрывов без разрушения блоков
        new BukkitRunnable() {
            @Override
            public void run() {
                // Создаём безопасные взрывы в центре (без разрушения блоков)
                world.createExplosion(targetLoc, 4.0f, false, false, player);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), fuseTicks + 10);
        
        lastUseTimeRing.put(player.getUniqueId(), now);
    }

    private boolean isOrbitalCannon(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Орбитальная пушка");
    }
}