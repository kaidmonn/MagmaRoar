package com.example.dragonstaff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StaffItem {

    public static ItemStack createStaff() {
        ItemStack staff = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = staff.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Посох Дракона").color(TextColor.color(255, 215, 0)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("ПКМ: Призвать дракона или атаковать").color(TextColor.color(200, 200, 200)));
            lore.add(Component.text("ПКМ по дракону: Сесть").color(TextColor.color(100, 255, 100)));
            lore.add(Component.text("ЛКМ по дракону: Слезть").color(TextColor.color(255, 100, 100)));
            lore.add(Component.text("Пробел: Взлететь").color(TextColor.color(100, 255, 100)));
            lore.add(Component.text("Shift: Пикировать").color(TextColor.color(255, 100, 100)));
            lore.add(Component.text("F: Режим зависания").color(TextColor.color(255, 255, 100)));
            meta.lore(lore);

            staff.setItemMeta(meta);
        }
        return staff;
    }
}