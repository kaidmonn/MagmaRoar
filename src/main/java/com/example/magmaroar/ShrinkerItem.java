package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShrinkerItem {

    public static ItemStack createShrinker() {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§b§lУменьшитель"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Shift+ЛКМ: уменьшить себя (1 блок, 30 сек)"));
            lore.add(Component.text("§7Зажать ПКМ: увеличить врага (3 блока, 30 сек)"));
            lore.add(Component.text("§7Увеличенный враг: замедление + 8❤"));
            lore.add(Component.text("§7Кулдаун: 45 сек"));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }
}