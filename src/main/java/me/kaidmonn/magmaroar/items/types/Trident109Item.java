package me.kaidmonn.magmaroar.items.types;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import java.util.UUID;

public class Trident109Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Трезубец Посейдона"));
        meta.setCustomModelData(109);
        meta.setUnbreakable(true);
        
        // Урон 14 (База трезубца 9, добавляем 5)
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, 
            new AttributeModifier(UUID.randomUUID(), "trident_damage", 5.0, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        // Скорость атаки 1.6 (База ~1.1, добавляем 0.5)
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, 
            new AttributeModifier(UUID.randomUUID(), "trident_speed", 0.5, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}