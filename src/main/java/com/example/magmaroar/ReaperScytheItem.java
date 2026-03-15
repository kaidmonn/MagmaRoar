package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReaperScytheItem {

    public static ItemStack createScythe() {
        ItemStack scythe = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = scythe.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5Коса жнеца"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 0"));
            lore.add(Component.text("§7При ударе: крадёт все эффекты цели"));
            lore.add(Component.text("§7Кулдаун: 80 секунд"));
            meta.lore(lore);

            scythe.setItemMeta(meta);
        }
        return scythe;
    }
}