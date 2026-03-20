package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LudoSwordItem {

    // Метод для создания ItemStack (для рандомных команд)
    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5§lЛудо-меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7ПКМ: Крутить рулетку (2 сек)"));
            lore.add(Component.text("§7Шанс джекпота: 5%"));
            lore.add(Component.text("§711 предметов"));
            meta.lore(lore);

            meta.setCustomModelData(1004);
            sword.setItemMeta(meta);
        }
        return sword;
    }
    
    // Метод для выдачи через команду (для /ludo)
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