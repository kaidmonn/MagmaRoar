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

public class ShadowBlade107Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("Теневой клинок"));
        meta.setCustomModelData(107);
        meta.setUnbreakable(true);
        
        // Урон 14
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
            new AttributeModifier(UUID.randomUUID(), "shadow_damage", 6.0, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}