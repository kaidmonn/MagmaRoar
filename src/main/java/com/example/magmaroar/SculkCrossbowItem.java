package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SculkCrossbowItem {

    public static ItemStack createCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§3Скалковый арбалет"));
            
            meta.setUnbreakable(true);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Выстреливает 3 стрелы одновременно"));
            lore.add(Component.text("§7При попадании: мощный взрыв"));
            lore.add(Component.text("§7Убивает в полном незерите с защитой 4"));
            lore.add(Component.text("§7Взрывы не ломают блоки"));
            lore.add(Component.text("§7Кулдаун: 2 минуты"));
            meta.lore(lore);

            crossbow.setItemMeta(meta);
        }
        return crossbow;
    }
}