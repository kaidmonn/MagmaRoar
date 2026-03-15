package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArtemisBowItem {

    public static ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6Лук Артемиды"));
            
            // Устанавливаем урон 12
            meta.setCustomModelData(12);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 12"));
            lore.add(Component.text("§7Самонаводка: 50% (радиус 70 блоков)"));
            lore.add(Component.text("§7Типы стрел:"));
            lore.add(Component.text("  §b65% §7- ⚡ Молния"));
            lore.add(Component.text("  §c25% §7- 🔥 Огонь 5×5"));
            lore.add(Component.text("  §e10% §7- 💥 Взрыв (ур.4)"));
            lore.add(Component.text("  §6§l5% §7- 👑 Королевская (ВСЁ СРАЗУ)"));
            lore.add(Component.text("§7Требует обычные стрелы"));
            meta.lore(lore);

            bow.setItemMeta(meta);
        }
        return bow;
    }
}