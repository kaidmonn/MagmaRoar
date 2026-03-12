package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LightMaceHandler implements Listener {

    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private static final long JUMP_COOLDOWN = 15 * 1000; // 15 секунд
    private static final double JUMP_HEIGHT = 1.2; // 20 блоков (примерно)

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLightMace(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Проверка кулдауна
            long currentTime = System.currentTimeMillis();
            Long lastJump = lastJumpTime.get(player.getUniqueId());
            
            if (lastJump != null && currentTime - lastJump < JUMP_COOLDOWN) {
                long secondsLeft = (JUMP_COOLDOWN - (currentTime - lastJump)) / 1000;
                player.sendMessage("§cПрыжок перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            lastJumpTime.put(player.getUniqueId(), currentTime);
            
            // Подбрасываем вверх на 20 блоков
            player.setVelocity(player.getVelocity().add(new Vector(0, JUMP_HEIGHT, 0)));
            player.sendMessage("§aПрыжок! Готовь булаву!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Иммунитет к урону от падения, если в руке булава
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            if (isLightMace(mainHand) || isLightMace(offHand)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isLightMace(ItemStack item) {
        if (item != null && item.getType() == Material.MACE && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.displayName() != null && 
                   meta.displayName().toString().contains("Легкая Булава");
        }
        return false;
    }
}