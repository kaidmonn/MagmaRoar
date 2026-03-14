package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HellMeteorItem {

    public static ItemStack createMeteor() {
        ItemStack meteor = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = meteor.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cАдский метеорит"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Призвать метеорит"));
            lore.add(Component.text("§7Падает 2 секунды"));
            lore.add(Component.text("§7Взрыв уровня 7, радиус 7 блоков"));
            lore.add(Component.text("§7Призывает 4 больших магма-куба"));
            lore.add(Component.text("§7Магма-кубы не атакуют владельца"));
            lore.add(Component.text("§7Кулдаун: 30 секунд"));
            meta.lore(lore);

            meteor.setItemMeta(meta);
        }
        return meteor;
    }
}