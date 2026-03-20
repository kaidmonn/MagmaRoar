package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PoseidonTridentItem {

    public static ItemStack createTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§b⚡ Трезубец Посейдона"));
            meta.setCustomModelData(3002);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7Скорость атаки: 1.6"));
            lore.add(Component.text("§7Shift+ПКМ: Рывок Посейдона (ударная волна)"));
            lore.add(Component.text("§7ПКМ: Бросок с шансом молнии"));
            lore.add(Component.text("§7Молния наносит чистый урон"));
            meta.lore(lore);

            trident.setItemMeta(meta);
        }
        return trident;
    }
}