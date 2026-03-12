package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Плотность: 4"));
            lore.add(Component.text("§7Порыв ветра: 2"));
            lore.add(Component.text("§7ПКМ: Подбрасывает вверх"));
            lore.add(Component.text("§7Каждые 20 сек: сносит щит"));
            meta.lore(lore);

            mace.setItemMeta(meta);
        }
        return mace;
    }
}