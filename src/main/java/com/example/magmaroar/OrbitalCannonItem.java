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
            lore.add(Component.text("§7ПКМ: 5 мгновенных взрывов (по взгляду)"));
            lore.add(Component.text("§7Shift+ЛКМ: 360 ТНТ кольцами вокруг игрока"));
            lore.add(Component.text("§7Радиусы: 15, 21, 27, 33, 39 блоков"));
            lore.add(Component.text("§7ТНТ в 3 блоках друг от друга"));
            lore.add(Component.text("§7Взрывы не ломают блоки"));
            lore.add(Component.text("§7ПКМ кулдаун: 25 сек"));
            lore.add(Component.text("§7ЛКМ кулдаун: 3 минуты"));
            lore.add(Component.text("§7Нерушимая"));
            meta.lore(lore);

            cannon.setItemMeta(meta);
        }
        return cannon;
    }
}