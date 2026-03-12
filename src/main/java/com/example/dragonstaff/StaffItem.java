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
            meta.displayName(Component.text("§6Посох Дракона"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Призвать дракона или атаковать"));
            lore.add(Component.text("§aПКМ по дракону: Сесть"));
            lore.add(Component.text("§cЛКМ по дракону: Слезть"));
            lore.add(Component.text("§aПробел: Взлететь"));
            lore.add(Component.text("§cShift: Пикировать"));
            lore.add(Component.text("§eF: Режим зависания"));
            lore.add(Component.text("§7Кулдаун призыва: 3 минуты"));
            lore.add(Component.text("§7Дракон исчезает через 90 секунд"));
            meta.lore(lore);

            staff.setItemMeta(meta);
        }
        return staff;
    }
}