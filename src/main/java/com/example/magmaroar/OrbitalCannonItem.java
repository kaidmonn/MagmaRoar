package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OrbitalCannonItem {

    public static ItemStack createCannon() {
        ItemStack cannon = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = cannon.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5Орбитальная пушка"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Вызывает 5 ТНТ в точку взгляда"));
            lore.add(Component.text("§7Взрывы не ломают блоки"));
            lore.add(Component.text("§7Кулдаун: 25 секунд"));
            lore.add(Component.text("§7Нерушимая"));
            meta.lore(lore);

            cannon.setItemMeta(meta);
        }
        return cannon;
    }
}