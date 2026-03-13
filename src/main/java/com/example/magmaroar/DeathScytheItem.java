package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DeathScytheItem {

    public static ItemStack createScythe() {
        ItemStack scythe = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = scythe.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§8Коса смерти"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7При ударе: 5 сердец урона (игнорит броню)"));
            lore.add(Component.text("§7Владелец получает 5 сердец здоровья"));
            lore.add(Component.text("§7Кулдаун: 40 секунд"));
            lore.add(Component.text("§7Частицы душ и звук визера"));
            lore.add(Component.text("§7Нерушимая"));
            meta.lore(lore);

            scythe.setItemMeta(meta);
        }
        return scythe;
    }
}