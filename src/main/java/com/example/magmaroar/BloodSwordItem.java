package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BloodSwordItem {

    // Метод для создания ItemStack (для рандомных команд)
    public static ItemStack createBloodSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cКровавый меч"));
            meta.setCustomModelData(1001);  // ← ЧИСЛО!

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7Shift+ПКМ: переключение режима"));
            lore.add(Component.text("§7Режимы: Меч → Трезубец → Булава"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
    
    // Метод для выдачи игроку
    public static void giveBloodSword(Player player) {
        player.getInventory().addItem(createBloodSword());
        player.sendMessage("§aВы получили Кровавый меч!");
    }
}