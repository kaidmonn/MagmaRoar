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

public class BloodSword201Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Кровавый меч"));
        meta.setCustomModelData(201);
        meta.setUnbreakable(true);
        
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
            new AttributeModifier(UUID.randomUUID(), "blood_damage", 6.0, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}