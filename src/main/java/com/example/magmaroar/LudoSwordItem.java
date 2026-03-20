package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LudoSwordItem {

    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5§lЛудо-меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7ПКМ: Крутить рулетку (2 сек)"));
            lore.add(Component.text("§7Шанс джекпота: 5%"));
            lore.add(Component.text("§711 предметов"));
            meta.lore(lore);

            // УСТАНАВЛИВАЕМ ТВОЮ МОДЕЛЬ 1004
            meta.setCustomModelData(1004); 
            
            sword.setItemMeta(meta);
        }
        return sword;
    }
}