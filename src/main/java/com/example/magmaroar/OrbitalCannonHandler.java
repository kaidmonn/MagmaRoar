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

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleNormalMode(player);
            event.setCancelled(true);
        }
        
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
        
        Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 0, 0.5);
        spawnInstantTNT(player, targetLoc);
        player.sendMessage("§5Орбитальная пушка: 5 ТНТ сброшены!");
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
        spawnWembuRingTNT(player, targetLoc);
        player.sendMessage("§5§lКОЛЬЦЕВОЙ РЕЖИМ АКТИВИРОВАН! 100+ ТНТ СБРОШЕНЫ!");
        lastUseTimeRing.put(player.getUniqueId(), now);
    }

    private void spawnInstantTNT(Player player, Location center) {
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = center.clone().add(0, 5 + i, 0);
            TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);
            tnt.setFuseTicks(40);
            tnt.setYield(4.0f);
            tnt.setIsIncendiary(false);
            tnt.setGlowing(true);
            tnt.setVelocity(new org.bukkit.util.Vector(0, -0.5, 0));
        }
    }

    private void spawnWembuRingTNT(Player player, Location center) {
        World world = player.getWorld();
        
        int rings = 5;
        int tntPerRing = 24;
        double baseRadius = 3.0;
        double radiusIncrease = 3.0;
        double heightBase = 20.0;
        double heightDecrease = 3.0;
        
        for (int ring = 0; ring < rings; ring++) {
            double radius = baseRadius + (ring * radiusIncrease);
            double height = heightBase - (ring * heightDecrease);
            int tntCount = tntPerRing + (ring * 4);
            
            for (int i = 0; i < tntCount; i++) {
                double angle = 2 * Math.PI * i / tntCount;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                
                Location tntLoc = new Location(world, x, center.getY() + height, z);
                TNTPrimed tnt = world.spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(40 + ring * 5);
                tnt.setYield(4.0f);
                tnt.setIsIncendiary(false);
                tnt.setGlowing(true);
                tnt.setVelocity(new org.bukkit.util.Vector(
                    (Math.random() - 0.5) * 0.2,
                    -0.3,
                    (Math.random() - 0.5) * 0.2
                ));
            }
        }
        
        TNTPrimed centerTNT = world.spawn(center.clone().add(0, 25, 0), TNTPrimed.class);
        centerTNT.setFuseTicks(50);
        centerTNT.setYield(6.0f);
        centerTNT.setIsIncendiary(false);
        centerTNT.setGlowing(true);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                world.createExplosion(center, 4.0f, false, false, player);
                for (int ring = 0; ring < rings; ring++) {
                    double radius = baseRadius + (ring * radiusIncrease);
                    int tntCount = tntPerRing + (ring * 4);
                    for (int i = 0; i < tntCount; i++) {
                        double angle = 2 * Math.PI * i / tntCount;
                        double x = center.getX() + radius * Math.cos(angle);
                        double z = center.getZ() + radius * Math.sin(angle);
                        Location explodeLoc = new Location(world, x, center.getY(), z);
                        world.createExplosion(explodeLoc, 4.0f, false, false, player);
                    }
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 45L);
    }

    private boolean isOrbitalCannon(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Орбитальная пушка");
    }
}