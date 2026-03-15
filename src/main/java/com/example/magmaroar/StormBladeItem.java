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
            lore.add(Component.text("§715% шанс: подброс 8 блоков + молния"));
            lore.add(Component.text("§7ПКМ: 10 молний-снарядов"));
            lore.add(Component.text("§7Кулдаун: 30 секунд"));
            meta.lore(lore);

            blade.setItemMeta(meta);
        }
        return blade;
    }
}