package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MirrorSwordItem {

    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§f§lЗеркальный меч"));
            meta.setCustomModelData(2002);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7Shift+ПКМ по игроку: выдать копию (макс 5)"));
            lore.add(Component.text("§7ПКМ: бафф союзников (спешка 2 + скорость 3)"));
            lore.add(Component.text("§7Владелец светится во время баффа"));
            lore.add(Component.text("§7Кулдаун: 60 сек"));
            meta.lore(lore);

            sword.setItemMeta(meta);
        }
        return sword;
    }
}