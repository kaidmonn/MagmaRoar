package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TimeBowItem {

    public static ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bЛук времени"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7При попадании: метка на 20 секунд (зелёное свечение)"));
            lore.add(Component.text("§7Shift+ПКМ: цель возвращается в прошлое"));
            lore.add(Component.text("§7Цель повторяет свои действия задом наперёд"));
            lore.add(Component.text("§7Эндер-жемчуг не спасает"));
            lore.add(Component.text("§7Кулдаун: 50 секунд"));
            meta.lore(lore);

            bow.setItemMeta(meta);
        }
        return bow;
    }
}