package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    private static final long COOLDOWN_NORMAL = 25 * 1000;
    private static final long COOLDOWN_RING = 3 * 60 * 1000;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isOrbitalCannon(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.isSneaking()) {
                handleNormalMode(player);
                event.setCancelled(true);
            }
        }

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

        Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 0, 0.5);
        World world = player.getWorld();

        int tntCount = 3;
        double spread = 1.5;

        for (int level = 0; level < 8; level++) {
            double yOffset = level * 3;

            for (int i = 0; i < tntCount; i++) {
                double xOffset = (Math.random() - 0.5) * spread;
                double zOffset = (Math.random() - 0.5) * spread;

                Location tntLoc = targetLoc.clone().add(xOffset, yOffset, zOffset);
                
                TNTPrimed tnt = world.spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(1);
                tnt.setYield(8.0f); // Увеличен урон с 4.0 до 8.0 (5 сердец)
                tnt.setIsIncendiary(false);
                tnt.setGlowing(true);
            }
        }

        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.5f);
        player.sendMessage("§5Орбитальная пушка: 24 ТНТ (урон 5♥)!");
        
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

        Location center = player.getLocation().clone();
        World world = player.getWorld();
        double height = 30.0;

        int[] tntPerRing = {72, 90, 108, 126, 144};
        double[] radii = {15.0, 21.0, 27.0, 33.0, 39.0};
        float yield = 12.0f; // Увеличен урон (было 6.0)

        player.sendMessage("§5§lКОЛЬЦЕВОЙ РЕЖИМ! " + (72+90+108+126+144) + " ТНТ ПАДАЕТ С НЕБА!");

        for (int ring = 0; ring < 5; ring++) {
            double radius = radii[ring];
            int count = tntPerRing[ring];

            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                double yOffset = (Math.random() - 0.5) * 2;
                Location tntLoc = new Location(world, x, center.getY() + height + yOffset, z);

                TNTPrimed tnt = world.spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(40 + ring * 5);
                tnt.setYield(yield);
                tnt.setIsIncendiary(false);
                tnt.setGlowing(true);
            }
        }

        Location centerLoc = new Location(world, center.getX(), center.getY() + height + 5, center.getZ());
        TNTPrimed centerTNT = world.spawn(centerLoc, TNTPrimed.class);
        centerTNT.setFuseTicks(50);
        centerTNT.setYield(yield * 1.5f);
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