package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FossilSwordItem {

    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6Ископаемый меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7ПКМ: Все эффекты тотема на 20 сек"));
            lore.add(Component.text("§7Пассивно: Срабатывает как тотем"));
            lore.add(Component.text("§7При смерти меч исчезает, но спасает жизнь"));
            lore.add(Component.text("§7Кулдаун: 75 секунд"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}