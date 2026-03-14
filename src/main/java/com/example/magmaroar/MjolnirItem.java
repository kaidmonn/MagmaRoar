package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MjolnirItem {

    public static ItemStack createMjolnir() {
        ItemStack mjolnir = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = mjolnir.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bМьёльнир"));
            meta.setUnbreakable(true);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ЛКМ: Удар по области (КД 3 сек)"));
            lore.add(Component.text("§7ПКМ: Бросок (КД 20 сек)"));
            lore.add(Component.text("§7Урон 2.5♥ всем в радиусе 5 блоков"));
            lore.add(Component.text("§7Владелец не получает урон"));
            lore.add(Component.text("§7Молния (визуал) при ударе/попадании"));
            meta.lore(lore);

            mjolnir.setItemMeta(meta);
        }
        return mjolnir;
    }
}