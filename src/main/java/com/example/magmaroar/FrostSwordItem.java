package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FrostSwordItem {

    public static ItemStack createFrostSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bМорозный меч"));
            meta.setCustomModelData(1005);  // ← ЭТО САМОЕ ГЛАВНОЕ!
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Замораживает врагов"));
            lore.add(Component.text("§715 ударов до заморозки"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}