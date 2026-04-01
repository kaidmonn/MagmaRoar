package me.kaidmonn.magmaroar.items.types;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class Mace106Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Легкая булава"));
        meta.setCustomModelData(106);
        meta.setUnbreakable(true);
        
        // Добавляем зачарования
        meta.addEnchant(Enchantment.DENSITY, 5, true);
        meta.addEnchant(Enchantment.WIND_BURST, 2, true);
        
        // Скрываем зачарования и флаг неразрушимости
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        
        item.setItemMeta(meta);
        return item;
    }
}