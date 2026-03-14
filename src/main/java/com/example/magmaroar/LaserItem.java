package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LaserItem {

    public static ItemStack createLaser() {
        ItemStack laser = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = laser.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§eЛазер"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Активировать лазер"));
            lore.add(Component.text("§7Жёлтый луч наводится на цель"));
            lore.add(Component.text("§7Урон 1 сердце в секунду"));
            lore.add(Component.text("§7Поджигает цель на 20 сек"));
            lore.add(Component.text("§7Огонь не тушится"));
            lore.add(Component.text("§7Кулдаун: 60 секунд"));
            meta.lore(lore);

            laser.setItemMeta(meta);
        }
        return laser;
    }
}