package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TimeClockItem {

    public static ItemStack createClock() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6Часы времени"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Создать временной пузырь (7×7×7)"));
            lore.add(Component.text("§7Внутри пузыря всё замирает на 7 секунд"));
            lore.add(Component.text("§7Игроки замораживаются, мобы стоят, снаряды зависают"));
            lore.add(Component.text("§7Эндер-жемчуг не работает"));
            lore.add(Component.text("§7Кулдаун: 90 секунд"));
            meta.lore(lore);

            clock.setItemMeta(meta);
        }
        return clock;
    }
}