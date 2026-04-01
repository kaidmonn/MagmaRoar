package me.yourname.ultimateitems.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.yourname.ultimateitems.UltimateItems;

public class ItemManager {

    public static ItemStack getItem(int cmd) {
        // Устанавливаем базовый материал
        Material mat = Material.NETHERITE_SWORD;

        if (cmd == 101 || cmd == 102) mat = Material.NETHERITE_HOE;
        else if (cmd == 103) mat = Material.DIAMOND_AXE;
        else if (cmd == 104) mat = Material.CROSSBOW;
        else if (cmd == 106) mat = Material.MACE;
        else if (cmd == 108) mat = Material.BLAZE_ROD;
        else if (cmd == 109) mat = Material.TRIDENT;
        // ИСПРАВЛЕНО: DIAMOND вместо ALAMOND
        else if (cmd >= 204 && cmd <= 206) mat = (cmd == 205 || cmd == 206) ? Material.DIAMOND_SWORD : Material.NETHERITE_SWORD;
        else if (cmd == 207) mat = Material.BOW;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setCustomModelData(cmd);
        meta.setUnbreakable(true);
        meta.setDisplayName("§6Ultimate Item #" + cmd);

        // ИСПРАВЛЕНО: Логика атрибутов для 1.21.4
        if (cmd == 105 || cmd == 107 || (cmd >= 201 && cmd <= 204) || cmd == 109) {
            // В 1.21.4 используется Attribute.ATTACK_DAMAGE вместо GENERIC_ATTACK_DAMAGE
            NamespacedKey key = new NamespacedKey(UltimateItems.getInstance(), "extra_damage_" + cmd);
            AttributeModifier modifier = new AttributeModifier(
                key, 
                6.0, 
                AttributeModifier.Operation.ADD_NUMBER, 
                EquipmentSlotGroup.MAINHAND
            );
            
            // Удаляем старые модификаторы, чтобы они не стакались, и добавляем новый
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
        }

        item.setItemMeta(meta);
        return item;
    }
}