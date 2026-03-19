package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LudoSwordItem {

    public static void giveLudoSword(Player player) {
        String command = "give " + player.getName() + " minecraft:netherite_sword[" +
            "custom_model_data={strings:[\"1004\"]}," +
            "item_name='{\"text\":\"Лудо-меч\",\"color\":\"light_purple\",\"bold\":true}'," +
            "lore=['{\"text\":\"ПКМ: запустить рулетку\",\"color\":\"gray\"}']" +
            "] 1";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}