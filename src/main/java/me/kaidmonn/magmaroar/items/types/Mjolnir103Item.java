package me.kaidmonn.magmaroar.items.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class Mjolnir103Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Мьёльнир"));
        meta.setCustomModelData(103);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        item.setItemMeta(meta);
        return item;
    }
}