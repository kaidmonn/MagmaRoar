package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
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

    private final Map<UUID, Long> lastUseTime = new HashMap<>();
    private static final long COOLDOWN = 25 * 1000; // 25 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isOrbitalCannon(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = lastUseTime.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cОрбитальная пушка перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Получаем точку взгляда игрока
            Location targetLoc = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);
            
            // Проверяем, зажат ли Shift
            if (player.isSneaking()) {
                // Режим 2: Кольцевой динамит (как у Wembu)
                spawnRingTNT(player, targetLoc);
                player.sendMessage("§5Орбитальная пушка: кольцевой режим активирован!");
            } else {
                // Режим 1: Обычный - 5 ТНТ в одной точке (мгновенный взрыв)
                spawnInstantTNT(player, targetLoc);
                player.sendMessage("§5Орбитальная пушка: 5 ТНТ сброшены!");
            }
            
            lastUseTime.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    private void spawnInstantTNT(Player player, Location center) {
        for (int i = 0; i < 5; i++) {
            TNTPrimed tnt = player.getWorld().spawn(center, TNTPrimed.class);
            tnt.setFuseTicks(0); // Мгновенный взрыв
            tnt.setYield(4.0f);
            tnt.setIsIncendiary(false);
            tnt.setGlowing(true);
            
            // Сразу создаём взрыв без разрушения блоков
            player.getWorld().createExplosion(center, 4.0f, false, false, player);
        }
    }

    private void spawnRingTNT(Player player, Location center) {
        int radius = 5;
        int tntCount = 12;
        
        for (int i = 0; i < tntCount; i++) {
            double angle = 2 * Math.PI * i / tntCount;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location tntLoc = new Location(center.getWorld(), x, center.getY(), z);
            TNTPrimed tnt = player.getWorld().spawn(tntLoc, TNTPrimed.class);
            tnt.setFuseTicks(0); // Мгновенный взрыв
            tnt.setYield(4.0f);
            tnt.setIsIncendiary(false);
            tnt.setGlowing(true);
            
            // Сразу создаём взрыв без разрушения блоков
            player.getWorld().createExplosion(tntLoc, 4.0f, false, false, player);
        }
    }

    private boolean isOrbitalCannon(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Орбитальная пушка");
    }
}