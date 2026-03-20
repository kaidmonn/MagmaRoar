package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LudoSwordItem {

    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5§lЛудо-меч"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7ПКМ: Крутить рулетку (2 сек)"));
            lore.add(Component.text("§7Шанс джекпота: 5%"));
            lore.add(Component.text("§711 предметов: морозный, теневой, паучий, мьёльнир,"));
            lore.add(Component.text("§7коса смерти, клинок бури, коса жнеца, катана,"));
            lore.add(Component.text("§7экскалибур, легкая булава, ДЖЕКПОТ"));
            meta.lore(lore);

            // ВАЖНО: custom_model_data для модели
            meta.setCustomModelData(1004);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}