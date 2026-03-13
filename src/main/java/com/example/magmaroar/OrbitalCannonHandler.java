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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrbitalCannonHandler implements Listener {

    private final Map<UUID, Long> lastUseTimeNormal = new HashMap<>();
    private final Map<UUID, Long> lastUseTimeRing = new HashMap<>();
    private static final long COOLDOWN_NORMAL = 25 * 1000;
    private static final long COOLDOWN_RING = 3 * 60 * 1000;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isOrbitalCannon(item)) return;

        // ПКМ - обычный режим
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.isSneaking()) {
                handleNormalMode(player);
                event.setCancelled(true);
            }
        }
        
        // Shift+ЛКМ - кольцевой режим
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                handleRingMode(player);
                event.setCancelled(true);
            }
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
        
        // ИДЕАЛЬНЫЕ КОЛЬЦА
        int rings = 5; // 5 колец
        int tntPerRing = 36; // Больше ТНТ для идеального круга
        double baseRadius = 10.0; // Первое кольцо на 10 блоках
        double radiusIncrease = 8.0; // Шаг между кольцами
        double height = 30.0; // Высота спавна
        
        player.sendMessage("§5§lКОЛЬЦЕВОЙ РЕЖИМ! 180+ ТНТ ПАДАЕТ С НЕБА!");
        
        for (int ring = 0; ring < rings; ring++) {
            double radius = baseRadius + (ring * radiusIncrease);
            
            for (int i = 0; i < tntPerRing; i++) {
                // Идеальный круг через синус/косинус
                double angle = 2 * Math.PI * i / tntPerRing;
                double x = targetLoc.getX() + radius * Math.cos(angle);
                double z = targetLoc.getZ() + radius * Math.sin(angle);
                
                Location tntLoc = new Location(world, x, targetLoc.getY() + height, z);
                TNTPrimed tnt = world.spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(40 + ring * 5);
                tnt.setYield(4.0f);
                tnt.setIsIncendiary(false);
                tnt.setGlowing(true);
                
                // Вертикальная скорость для падения
                tnt.setVelocity(new org.bukkit.util.Vector(0, -0.3, 0));
            }
        }
        
        // Центральный мощный ТНТ
        Location centerLoc = new Location(world, targetLoc.getX(), targetLoc.getY() + height + 5, targetLoc.getZ());
        TNTPrimed centerTNT = world.spawn(centerLoc, TNTPrimed.class);
        centerTNT.setFuseTicks(50);
        centerTNT.setYield(6.0f);
        centerTNT.setIsIncendiary(false);
        centerTNT.setGlowing(true);
        
        lastUseTimeRing.put(player.getUniqueId(), now);
    }

    private boolean isOrbitalCannon(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Орбитальная пушка");
    }
}