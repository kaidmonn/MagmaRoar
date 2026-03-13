package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TestZombieItem {

    public static ItemStack createZombieEgg(boolean withTotems) {
        ItemStack egg = new ItemStack.Material.ZOMBIE_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();

        if (meta != null) {
            if (withTotems) {
                meta.displayName(Component.text("§cЗомби-бессмертный"));
            } else {
                meta.displayName(Component.text("§6Зомби-щитоносец"));
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Призыв тестового зомби"));
            lore.add(Component.text("§7Незеритовая броня (защита 4)"));
            if (withTotems) {
                lore.add(Component.text("§cБесконечные тотемы бессмертия"));
            } else {
                lore.add(Component.text("§6Всегда использует щит"));
            }
            meta.lore(lore);

            egg.setItemMeta(meta);
        }
        return egg;
    }
}