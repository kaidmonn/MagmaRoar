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

        // ПКМ - обычный режим (5 взрывов по взгляду)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.isSneaking()) {
                handleNormalMode(player);
                event.setCancelled(true);
            }
        }
        
        // Shift+ЛКМ - кольцевой режим (540 ТНТ вокруг игрока)
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
        
        Location center = player.getLocation().clone();
        World world = player.getWorld();
        double height = 35.0;
        
        // Кольца - 540 ТНТ
        int rings = 5;
        double[] radii = {15.0, 21.0, 27.0, 33.0, 39.0};
        int[] tntPerRing = {72, 90, 108, 126, 144}; // 540 ТНТ
        
        int totalTNT = 0;
        for (int count : tntPerRing) totalTNT += count;
        
        player.sendMessage("§5§lКОЛЬЦЕВОЙ РЕЖИМ! " + totalTNT + " ТНТ ПАДАЕТ С НЕБА!");
        player.sendMessage("§7Радиусы колец: 15, 21, 27, 33, 39 блоков");
        player.sendMessage("§7Вы в центре — вы в безопасности!");
        
        float yield = 6.0f;
        
        for (int ring = 0; ring < rings; ring++) {
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
                
                // ТНТ летят строго вниз (не разлетаются)
                tnt.setVelocity(new org.bukkit.util.Vector(0, -0.5, 0));
            }
        }
        
        // Центральный ТНТ УБРАН - владелец в безопасности
        
        lastUseTimeRing.put(player.getUniqueId(), now);
    }

    private boolean isOrbitalCannon(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Орбитальная пушка");
    }
}