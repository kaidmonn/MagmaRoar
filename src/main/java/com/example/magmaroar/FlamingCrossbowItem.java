package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FlamingCrossbowItem {

    public static ItemStack createCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cПылающий арбалет"));
            
            // Устанавливаем прочность через Damageable
            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                damageable.setMaxDamage(6); // Макс прочность 6
                damageable.setDamage(0); // Начинается с полной прочности
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