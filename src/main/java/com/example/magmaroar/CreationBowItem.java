package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CreationBowItem {

    public static ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§2Лук сотворения"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Создать лес 20×20"));
            lore.add(Component.text("§7Платформа из земли с барьерами"));
            lore.add(Component.text("§7Дубовый лес + 5 волков"));
            lore.add(Component.text("§7Волки: сила 5, скорость 3, хп x3"));
            lore.add(Component.text("§7Блоки нельзя ломать/ставить"));
            lore.add(Component.text("§7Живёт 25 секунд"));
            lore.add(Component.text("§7Кулдаун: 90 секунд"));
            meta.lore(lore);

            bow.setItemMeta(meta);
        }
        return bow;
    }
}