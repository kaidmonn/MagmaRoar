package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class ExplosivePotionItem {

    public static ItemStack createPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION, 16); // Стакается по 16
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§dВзрывное зелье"));
            
            // Добавляем эффекты на 3 минуты (3600 тиков)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 1), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1), true);
            
            // Делаем зелье цветным (фиолетовый)
            meta.setColor(org.bukkit.Color.fromRGB(200, 0, 200));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Сила II (3 минуты)"));
            lore.add(Component.text("§7Скорость II (3 минуты)"));
            lore.add(Component.text("§7Взрывное - действует на всех рядом"));
            lore.add(Component.text("§7Стакается по 16"));
            meta.lore(lore);

            potion.setItemMeta(meta);
        }
        return potion;
    }
}