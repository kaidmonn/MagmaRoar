package me.yourname.ultimateitems.items;

import org.bukkit.Material;
import org.bukkit.attribute.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class ItemManager {
    public static ItemStack getItem(int cmd) {
        Material mat = Material.NETHERITE_SWORD;
        String name = "Item " + cmd;
        double dmg = 0;

        if (cmd == 101 || cmd == 102) mat = Material.NETHERITE_HOE;
        if (cmd == 103) mat = Material.DIAMOND_AXE;
        if (cmd == 104) mat = Material.CROSSBOW;
        if (cmd == 106) mat = Material.MACE;
        if (cmd == 108) mat = Material.BLAZE_ROD;
        if (cmd == 109) mat = Material.TRIDENT;
        if (cmd >= 204 && cmd <= 206) mat = (cmd == 205 || cmd == 206) ? Material.ALAMOND_SWORD : Material.NETHERITE_SWORD;
        if (cmd == 207) mat = Material.BOW;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(cmd);
        meta.setUnbreakable(true);
        meta.setDisplayName("§6Ultimate Item #" + cmd);

        // Урон 14 для мечей (Базовый 8 + 6)
        if (cmd == 105 || cmd == 107 || (cmd >= 201 && cmd <= 204) || cmd == 109) {
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "damage", 6, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }
        
        item.setItemMeta(meta);
        return item;
    }
}