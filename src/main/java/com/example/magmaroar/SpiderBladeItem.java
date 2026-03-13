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

public class SpiderBladeItem {

    public static ItemStack createBlade() {
        ItemStack blade = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = blade.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§2Паучий клинок"));
            
            // Добавляем атрибут скорости +40%
            AttributeModifier speedModifier = new AttributeModifier(
                UUID.randomUUID(),
                "spider_blade_speed",
                0.4,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlot.HAND
            );
            meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, speedModifier);
            
            // Нерушимость
            meta.setUnbreakable(true);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Урон: 14"));
            lore.add(Component.text("§7При ударе: 7% шанс отравления II (20 сек)"));
            lore.add(Component.text("§7ПКМ: Паутина 5×5 (20 сек кулдаун)"));
            lore.add(Component.text("§7Владелец не застревает в паутине"));
            lore.add(Component.text("§7+40% к скорости передвижения"));
            meta.lore(lore);

            blade.setItemMeta(meta);
        }
        return blade;
    }
}