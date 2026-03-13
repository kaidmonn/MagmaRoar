package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MjolnirItem {

    public static ItemStack createMjolnir() {
        ItemStack mjolnir = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = mjolnir.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bМьёльнир"));
            
            meta.setUnbreakable(true);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Бросить молот"));
            lore.add(Component.text("§7При попадании: Молния (3 сердца)"));
            lore.add(Component.text("§7Игнорирует броню"));
            lore.add(Component.text("§7Возвращается к владельцу"));
            lore.add(Component.text("§7Кулдаун: 20 секунд"));
            meta.lore(lore);

            mjolnir.setItemMeta(meta);
        }
        return mjolnir;
    }
}