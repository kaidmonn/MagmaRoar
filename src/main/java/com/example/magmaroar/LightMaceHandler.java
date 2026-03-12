package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LightMaceHandler implements Listener {

    private final Map<UUID, Long> lastShieldBreak = new HashMap<>();
    private static final long SHIELD_COOLDOWN = 20 * 1000; // 20 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLightMace(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Подбрасываем вверх на 15 блоков
            player.setVelocity(player.getVelocity().add(new Vector(0, 1.5, 0)));
            player.sendMessage("§aПрыжок! Готовь булаву!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isLightMace(item)) return;

        // Проверка кулдауна на снос щита
        long currentTime = System.currentTimeMillis();
        Long lastBreak = lastShieldBreak.get(player.getUniqueId());
        
        if (lastBreak == null || currentTime - lastBreak >= SHIELD_COOLDOWN) {
            // Сносим щит (топор делает это автоматически)
            // Булава с плотностью 4 уже имеет эту механику
            lastShieldBreak.put(player.getUniqueId(), currentTime);
            player.sendMessage("§eЩитолом готов! Следующий через 20 сек.");
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