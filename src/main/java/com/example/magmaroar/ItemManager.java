package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class ItemManager {

    private final MagmaRoarPlugin plugin;

    public ItemManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createUnbreakableNetheriteSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.setDisplayName("§fНерушимый меч");
        sword.setItemMeta(meta);
        return sword;
    }

    public ItemStack createUnbreakableNetheriteArmor(Material type) {
        ItemStack armor = new ItemStack(type);
        ItemMeta meta = armor.getItemMeta();
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        
        if (type == Material.NETHERITE_HELMET) {
            meta.setDisplayName("§fНерушимый шлем");
        } else if (type == Material.NETHERITE_CHESTPLATE) {
            meta.setDisplayName("§fНерушимый нагрудник");
        } else if (type == Material.NETHERITE_LEGGINGS) {
            meta.setDisplayName("§fНерушимые поножи");
        } else if (type == Material.NETHERITE_BOOTS) {
            meta.setDisplayName("§fНерушимые ботинки");
        }
        
        armor.setItemMeta(meta);
        return armor;
    }

    public ItemStack createEnchantedGoldenApple() {
        return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
    }

    public ItemStack createGoldenApple() {
        return new ItemStack(Material.GOLDEN_APPLE);
    }

    public ItemStack createEnderPearl() {
        return new ItemStack(Material.ENDER_PEARL, 16);
    }

    public ItemStack createWindCharge() {
        return new ItemStack(Material.WIND_CHARGE, 16);
    }

    public ItemStack createCobweb() {
        return new ItemStack(Material.COBWEB, 16);
    }

    public ItemStack createShield() {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        shield.setItemMeta(meta);
        return shield;
    }

    public ItemStack createNetheriteAxe() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        axe.setItemMeta(meta);
        return axe;
    }

    public ItemStack createNetheritePickaxe() {
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        pickaxe.setItemMeta(meta);
        return pickaxe;
    }

    public ItemStack createWaterBucket() {
        return new ItemStack(Material.WATER_BUCKET);
    }

    public ItemStack createOakLogs() {
        return new ItemStack(Material.OAK_LOG, 64);
    }

    public ItemStack createArrows() {
        return new ItemStack(Material.ARROW, 64);
    }

    public ItemStack createMaceWithBreach() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        meta.addEnchant(Enchantment.BREACH, 4, true);
        meta.setDisplayName("§5Булава Пробития");
        mace.setItemMeta(meta);
        return mace;
    }

    public ItemStack createTotem() {
        return new ItemStack(Material.TOTEM_OF_UNDYING);
    }

    public ItemStack createCrownHelmet() {
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.setDisplayName("§6Корона Владыки");
        
        helmet.setItemMeta(meta);
        return helmet;
    }

    public ItemStack createMinecartTNT() {
        return new ItemStack(Material.TNT_MINECART, 6);
    }

    public ItemStack createFlameBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        meta.addEnchant(Enchantment.FLAME, 1, true);
        bow.setItemMeta(meta);
        return bow;
    }

    public ItemStack createRails() {
        return new ItemStack(Material.RAIL, 64);
    }
}