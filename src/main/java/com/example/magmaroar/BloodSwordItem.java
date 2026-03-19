package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BloodSwordItem {

    public static void giveBloodSword(Player player) {
        // Твоя рабочая команда
        String command = "give " + player.getName() + " minecraft:netherite_sword[" +
            "custom_model_data={strings:[\"1001\"]}," +
            "item_name='{\"text\":\"Кровавый меч\",\"color\":\"red\",\"bold\":true}'," +
            "lore=['{\"text\":\"Урон: 14\",\"color\":\"gray\"}'," +
                  "'{\"text\":\"Shift+ПКМ: переключение режима\",\"color\":\"gray\"}'," +
                  "'{\"text\":\"Режимы: Меч → Трезубец → Булава\",\"color\":\"gray\"}']" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}