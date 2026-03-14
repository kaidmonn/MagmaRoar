package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RavagerHornItem {

    public static ItemStack createHorn() {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cРог разорителя"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Призвать разорителя + заклинателя + иллюзиониста"));
            lore.add(Component.text("§7На разорителе можно ездить (WASD)"));
            lore.add(Component.text("§7ПКМ верхом: топот (урон 4♥ + замедление IV)"));
            lore.add(Component.text("§7Прислужники атакуют цель топата"));
            lore.add(Component.text("§7Живут 60 секунд"));
            lore.add(Component.text("§7Кулдаун: 2 минуты"));
            meta.lore(lore);

            horn.setItemMeta(meta);
        }
        return horn;
    }
}