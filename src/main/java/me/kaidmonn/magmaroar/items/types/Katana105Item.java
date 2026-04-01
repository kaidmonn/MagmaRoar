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

public class Katana105Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Катана дракона"));
        meta.setCustomModelData(105);
        meta.setUnbreakable(true);
        
        // Урон 14 (База 8 + Модификатор 6)
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
            new AttributeModifier(UUID.randomUUID(), "katana_damage", 6.0, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
            
        // Скорость +10%
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, 
            new AttributeModifier(UUID.randomUUID(), "katana_speed", 0.1, 
            AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
}