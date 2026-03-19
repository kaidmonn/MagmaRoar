package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BloodSwordItem {

    public static void giveBloodSword(Player player) {
        // Формируем команду с кавычками для строкового custom_model_data
        String command = "give " + player.getName() + " minecraft:netherite_sword[" +
            "custom_model_data={strings:[\"1001.0\"]}," +
            "item_name='\"§cКровавый меч\"'," +
            "lore=['\"§7Урон: 14\"','\"§7Shift+ПКМ: переключение режима\"','\"§7Режимы: Меч → Трезубец → Булава\"']" +
            "] 1";
        
        // Выполняем команду от имени консоли
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}