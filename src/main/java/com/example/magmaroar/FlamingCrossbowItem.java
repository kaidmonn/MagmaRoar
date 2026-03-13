package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FlamingCrossbowItem {

    public static ItemStack createCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cПылающий арбалет"));
            
            // Устанавливаем прочность 6
            meta.setMaxDamage(6);
            meta.setUnbreakable(false);
            
            // Автоматически заряжен (опционально)
            if (meta instanceof CrossbowMeta) {
                // Можно добавить стрелы, если хочешь
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Прочность: 6"));
            lore.add(Component.text("§7Стрелы поджигают цель"));
            lore.add(Component.text("§7Требует обычные стрелы"));
            meta.lore(lore);

            crossbow.setItemMeta(meta);
        }
        return crossbow;
    }
}