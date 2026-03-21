package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VillagerStaffItem {

    public static ItemStack createStaff() {
        ItemStack staff = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = staff.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§aПосох жителя"));
            
            // Устанавливаем кастомный ID предмета
            NamespacedKey key = new NamespacedKey(MagmaRoarPlugin.getInstance(), "village_staff");
            meta.setItemModel(key);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Создает мощный взрыв через 2 секунды"));
            lore.add(Component.text("§7Уровень взрыва: 20 (как энд-кристалл x3)"));
            lore.add(Component.text("§7Взрывы не ломают блоки"));
            lore.add(Component.text("§7Звук маяка при активации"));
            lore.add(Component.text("§7Кулдаун: 2 минуты"));
            meta.lore(lore);

            staff.setItemMeta(meta);
        }
        return staff;
    }
    
    public static void giveStaff(Player player) {
        player.getInventory().addItem(createStaff());
        player.sendMessage("§aВы получили Посох жителя!");
    }
}