package me.kaidmonn.magmaroar.items.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class Scythe101Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Коса Жнеца")); // Название без мистики
        meta.setCustomModelData(101);
        meta.setUnbreakable(true);
        
        // Скрываем стандартные теги неразрушимости
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        item.setItemMeta(meta);
        return item;
    }
}