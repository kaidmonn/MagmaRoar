package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class BloodSwordItem {

    public static void giveBloodSword(Player player) {
        // 1. Создаем основу предмета
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            // 2. Устанавливаем имя
            meta.setDisplayName("§cКровавый меч");

            // 3. Устанавливаем Custom Model Data (число 1001)
            // Это именно то, что ищет ваш JSON-файл модели
            meta.setCustomModelData(1001);

            // 4. Добавляем описание (Lore)
            meta.setLore(Arrays.asList(
                "§7Урон: 14",
                "§7Shift+ПКМ: переключение режима",
                "§7Режимы: Меч → Трезубец → Булава"
            ));

            // Применяем изменения к предмету
            sword.setItemMeta(meta);
        }

        // 5. Выдаем предмет игроку прямо в инвентарь
        player.getInventory().addItem(sword);
    }
}