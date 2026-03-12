package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MagmaHornItem {

    public static ItemStack createHorn() {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6Рог Магмы"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Призвать Магма Рёва"));
            lore.add(Component.text("§7ПКМ по Магма Рёву: Сесть"));
            lore.add(Component.text("§7Shift: Слезть"));
            lore.add(Component.text("§7Пробел: Прыжок (поджигает землю 3x3)"));
            lore.add(Component.text("§7ЛКМ/ПКМ (верхом): Атака TNT"));
            lore.add(Component.text("§7Кулдаун атаки: 20 сек"));
            lore.add(Component.text("§7Кулдаун призыва: 3 мин"));
            lore.add(Component.text("§7Магма Рёв живёт 90 сек"));
            meta.lore(lore);

            horn.setItemMeta(meta);
        }
        return horn;
    }
}