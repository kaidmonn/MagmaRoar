package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShadowSwordItem {

    public static ItemStack createShadowSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§8Теневой меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7ПКМ: Невидимость + Скорость III (30 сек)"));
            lore.add(Component.text("§7После 3 ударов эффекты пропадают"));
            lore.add(Component.text("§7Кулдаун: 60 секунд"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}