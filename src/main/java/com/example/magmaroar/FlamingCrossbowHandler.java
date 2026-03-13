package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FlamingCrossbowHandler implements Listener {

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        // Проверяем, что стрелок - игрок
        if (!(event.getEntity() instanceof Player)) return;
        
        // Проверяем, что это арбалет
        if (event.getBow() == null || event.getBow().getType() != Material.CROSSBOW) return;
        
        ItemStack crossbow = event.getBow();
        
        // Проверяем, что это наш Пылающий арбалет
        if (!isFlamingCrossbow(crossbow)) return;
        
        // Проверяем, что снаряд - стрела
        if (!(event.getProjectile() instanceof Arrow)) return;
        
        Arrow arrow = (Arrow) event.getProjectile();
        
        // Поджигаем стрелу на 5 секунд (100 тиков)
        arrow.setFireTicks(100);
        
        // Добавляем частицы огня (опционально)
        arrow.setGlowing(true);
        
        // Сообщение игроку (можно убрать если надоест)
        // player.sendMessage("§cСтрела горит!");
    }
    
    private boolean isFlamingCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Пылающий арбалет");
    }
}