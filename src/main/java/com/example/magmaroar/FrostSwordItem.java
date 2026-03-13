package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FrostSwordItem {

    public static ItemStack createFrostSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bМорозный меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7При ударе: замедление I на 3 сек"));
            lore.add(Component.text("§7После 15 ударов: полная заморозка на 4 сек"));
            lore.add(Component.text("§7Звук: ломающийся лёд"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}