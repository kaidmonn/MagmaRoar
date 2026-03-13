package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ExplosivePotionItem {

    public static ItemStack createPotion() {
        ItemStack snowball = new ItemStack(Material.SNOWBALL, 16); // Снежки стакаются по 16
        ItemMeta meta = snowball.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§dВзрывное зелье"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Бросай как снежок!"));
            lore.add(Component.text("§7Сила II (3 минуты)"));
            lore.add(Component.text("§7Скорость II (3 минуты)"));
            lore.add(Component.text("§7Взрывное - действует на всех рядом"));
            meta.lore(lore);

            snowball.setItemMeta(meta);
        }
        return snowball;
    }
}