package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KatanaItem {

    public static ItemStack createKatana() {
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§dКатана дракона"));

            // +15% скорости передвижения
            AttributeModifier speedModifier = new AttributeModifier(
                UUID.randomUUID(),
                "katana_speed",
                0.15,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlot.HAND
            );
            meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, speedModifier);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7+15% к скорости передвижения"));
            lore.add(Component.text("§7ПКМ: Телепортация (2 заряда)"));
            lore.add(Component.text("§7Кулдаун: 25 секунд"));
            meta.lore(lore);

            katana.setItemMeta(meta);
        }
        return katana;
    }
}