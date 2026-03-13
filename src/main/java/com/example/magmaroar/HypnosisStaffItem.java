package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HypnosisStaffItem {

    public static ItemStack createStaff() {
        ItemStack staff = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = staff.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§5Жезл гипноза"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7ПКМ: Призвать Вардена"));
            lore.add(Component.text("§7Варден следует за владельцем"));
            lore.add(Component.text("§7Атакует цель, которую били жезлом"));
            lore.add(Component.text("§7ПКМ с активным Варденом: телепорт"));
            lore.add(Component.text("§7Живёт 40 секунд"));
            lore.add(Component.text("§7Кулдаун: 90 секунд"));
            meta.lore(lore);

            staff.setItemMeta(meta);
        }
        return staff;
    }
}