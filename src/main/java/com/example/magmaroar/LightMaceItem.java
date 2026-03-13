package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LightMaceItem {

    public static ItemStack createMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bЛегкая Булава"));
            
            // Нерушимость
            meta.setUnbreakable(true);
            
            // Зачарования
            meta.addEnchant(Enchantment.DENSITY, 3, true);      // Плотность 3 (было 4)
            meta.addEnchant(Enchantment.WIND_BURST, 2, true);  // Порыв ветра 2

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Подбрасывает вверх (кулдаун 15 сек)"));
            lore.add(Component.text("§7Держа в руках: иммунитет к падению"));
            lore.add(Component.text("§7Плотность 3, Порыв ветра 2"));
            meta.lore(lore);

            mace.setItemMeta(meta);
        }
        return mace;
    }
}