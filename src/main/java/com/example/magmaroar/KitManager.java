package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class KitManager {

    private final MythicNPC plugin;
    private final Random random = new Random();

    public KitManager(MythicNPC plugin) {
        this.plugin = plugin;
    }

    public void giveKits(List<Player> players) {
        for (Player player : players) {
            giveBasicKit(player);
            
            // 30% шанс на доп. шалкер
            if (random.nextInt(100) < 30) {
                giveBonusShulker(player);
            }
        }
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }

    private void giveBasicKit(Player player) {
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = shulker.getItemMeta();
        meta.setDisplayName("§6§lБазовый кит");
        shulker.setItemMeta(meta);
        
        player.getInventory().addItem(shulker);
    }

    private void giveBonusShulker(Player player) {
        int type = random.nextInt(4);
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = shulker.getItemMeta();
        
        switch(type) {
            case 0:
                meta.setDisplayName("§5§lШалкер булавы");
                // Добавить булаву
                break;
            case 1:
                meta.setDisplayName("§c§lШалкер тотема");
                // Добавить тотем
                break;
            case 2:
                meta.setDisplayName("§6§lШалкер короны");
                // Добавить корону
                break;
            case 3:
                meta.setDisplayName("§2§lШалкер карт");
                // Добавить карты
                break;
        }
        
        shulker.setItemMeta(meta);
        player.getInventory().addItem(shulker);
    }
}