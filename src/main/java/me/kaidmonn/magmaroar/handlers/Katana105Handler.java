package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class Katana105Handler implements Listener {

    private final NamespacedKey chargesKey = new NamespacedKey(MagmaRoar.getInstance(), "katana_charges");

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 105) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Получаем текущие заряды (по умолчанию 2)
        int charges = item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(chargesKey, PersistentDataType.INTEGER, 2);

        if (charges <= 0) return;

        // Логика телепортации
        Location loc = p.getLocation();
        Vector direction = loc.getDirection().normalize().multiply(15);
        Location target = loc.add(direction);

        // Проверка, чтобы не телепортироваться в стену
        Block targetBlock = target.getBlock();
        if (targetBlock.getType().isSolid()) {
            target = findSafeLocation(target);
        }

        p.teleport(target);
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

        // Уменьшаем заряды
        updateCharges(item, charges - 1);
        
        // Визуальная шторка (15 сек)
        p.setCooldown(Material.NETHERITE_SWORD, 300);

        // Запуск восстановления заряда через 15 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack currentItem = p.getInventory().getItemInMainHand();
                if (isKatana(currentItem)) {
                    int currentCharges = currentItem.getItemMeta().getPersistentDataContainer()
                            .getOrDefault(chargesKey, PersistentDataType.INTEGER, 0);
                    if (currentCharges < 2) {
                        updateCharges(currentItem, currentCharges + 1);
                    }
                }
            }
        }.runTaskLater(MagmaRoar.getInstance(), 300);
    }

    private void updateCharges(ItemStack item, int count) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, count);
        item.setItemMeta(meta);
    }

    private Location findSafeLocation(Location loc) {
        // Простая проверка: поднимаем точку выше, если там блок
        while (loc.getBlock().getType().isSolid() && loc.getY() < 320) {
            loc.add(0, 1, 0);
        }
        return loc;
    }

    private boolean isKatana(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 105;
    }
}