package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StormBladeItem {

    public static ItemStack createBlade() {
        ItemStack blade = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = blade.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bКлинок бури"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7При ударе: 4% шанс подбросить на 8 блоков + молния"));
            lore.add(Component.text("§7ПКМ: Призвать 10 молний в точку взгляда"));
            lore.add(Component.text("§7Молнии взрываются уровнем 4"));
            lore.add(Component.text("§7Кулдаун: 20 секунд"));
            meta.lore(lore);

            blade.setItemMeta(meta);
        }
        return blade;
    }
}