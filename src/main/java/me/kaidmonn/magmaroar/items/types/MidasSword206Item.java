package me.kaidmonn.magmaroar.items.types;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import java.util.UUID;

public class MidasSword206Item {
    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§6Меч Мидаса"));
        meta.setCustomModelData(206);
        
        // Нерушимость
        meta.setUnbreakable(true);
        
        // Урон как у алмазного меча (7.0)
        // Базовый урон золотого меча 4.0, добавляем 3.0
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
            new AttributeModifier(UUID.randomUUID(), "midas_base_damage", 3.0, 
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        // Стартовая острота 16
        meta.addEnchant(Enchantment.SHARPNESS, 16, true);
        
        // Скрываем лишние теги
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
}