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
            lore.add(Component.text("§7ПКМ: 8 взрывов по вертикали"));
            lore.add(Component.text("§7Shift+ЛКМ: Кольцевой режим (540 ТНТ)"));
            lore.add(Component.text("§7Урон x1.5 в кольцевом режиме"));
            lore.add(Component.text("§7Взрывы не ломают блоки"));
            lore.add(Component.text("§7ПКМ кулдаун: 25 сек"));
            lore.add(Component.text("§7ЛКМ кулдаун: 3 минуты"));
            meta.lore(lore);

            cannon.setItemMeta(meta);
        }
        return cannon;
    }
}