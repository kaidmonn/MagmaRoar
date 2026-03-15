package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

public class KatanaHandler implements Listener {

    private final Map<UUID, KatanaInfo> playerInfo = new HashMap<>();
    
    private static final int MAX_CHARGES = 2;
    private static final long COOLDOWN = 25 * 1000; // 25 секунд
    private static final int TELEPORT_RANGE = 10;

    private static class KatanaInfo {
        int charges;
        long lastRegenTime;

        KatanaInfo() {
            this.charges = MAX_CHARGES;
            this.lastRegenTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isKatana(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            KatanaInfo info = playerInfo.computeIfAbsent(player.getUniqueId(), k -> new KatanaInfo());
            
            // Проверяем регенерацию зарядов
            long now = System.currentTimeMillis();
            if (info.charges < MAX_CHARGES && now - info.lastRegenTime >= COOLDOWN) {
                info.charges = MAX_CHARGES;
                info.lastRegenTime = now;
                player.sendMessage("§dЗаряды телепортации восстановлены!");
            }
            
            // Проверяем наличие зарядов
            if (info.charges <= 0) {
                long timeLeft = (COOLDOWN - (now - info.lastRegenTime)) / 1000;
                player.sendMessage("§cНет зарядов! Восстановление через: " + timeLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Получаем точку для телепортации
            Location targetLoc = player.getTargetBlock(null, TELEPORT_RANGE).getLocation().add(0.5, 1, 0.5);
            
            // Сохраняем старую позицию для эффектов
            Location oldLoc = player.getLocation();
            
            // Телепортируем
            player.teleport(targetLoc);
            
            // Уменьшаем заряды
            info.charges--;
            player.sendMessage("§dТелепортация! Осталось зарядов: " + info.charges + "/" + MAX_CHARGES);
            
            // Эффекты
            World world = player.getWorld();
            
            // На старом месте
            world.spawnParticle(Particle.DRAGON_BREATH, oldLoc, 30, 0.5, 0.5, 0.5, 0.1);
            world.playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            
            // На новом месте
            world.spawnParticle(Particle.DRAGON_BREATH, targetLoc, 30, 0.5, 0.5, 0.5, 0.1);
            
            event.setCancelled(true);
        }
    }

    private boolean isKatana(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Катана дракона");
    }
}