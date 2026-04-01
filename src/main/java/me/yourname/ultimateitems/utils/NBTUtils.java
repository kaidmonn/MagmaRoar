package me.yourname.ultimateitems.utils;

import me.yourname.ultimateitems.UltimateItems;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class NBTUtils {

    // Метод для записи числа (например, уровень остроты или стадия предмета)
    public static void setInt(ItemStack item, String key, int value) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(UltimateItems.getInstance(), key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    // Метод для получения числа
    public static int getInt(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(
                new NamespacedKey(UltimateItems.getInstance(), key), PersistentDataType.INTEGER, 0);
    }

    // Проверка наличия ключа
    public static boolean hasKey(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(UltimateItems.getInstance(), key), PersistentDataType.INTEGER);
    }
}