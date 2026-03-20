package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LudoSwordItem {

    public static void giveLudoSword(Player player) {
        String command = "give " + player.getName() + " minecraft:netherite_sword[" +
            "custom_model_data={strings:[\"1004\"]}," +
            "item_name='{\"text\":\"Лудо-меч\",\"color\":\"light_purple\",\"bold\":true}'," +
            "lore=['{\"text\":\"Урон: 14\",\"color\":\"gray\"}'," +
                  "'{\"text\":\"ПКМ: Крутить рулетку (2 сек)\",\"color\":\"gray\"}'," +
                  "'{\"text\":\"Шанс джекпота: 5%\",\"color\":\"gray\"}'," +
                  "'{\"text\":\"11 предметов\",\"color\":\"gray\"}']" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}