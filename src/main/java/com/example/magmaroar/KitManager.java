package com.example.magmaroar;

import org.bukkit.Bukkit; // ЭТОТ ИМПОРТ БЫЛ ПРОПУЩЕН!
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitManager {

    private final MagmaRoarPlugin plugin;
    private final Random random = new Random();

    public KitManager(MagmaRoarPlugin plugin) {
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
        ItemStack shulker = new ItemStack(org.bukkit.Material.SHULKER_BOX);
        var meta = shulker.getItemMeta();
        meta.setDisplayName("§6§lБазовый кит");
        shulker.setItemMeta(meta);
        player.getInventory().addItem(shulker);
    }

    private void giveBonusShulker(Player player) {
        int type = random.nextInt(4);
        ItemStack shulker = new ItemStack(org.bukkit.Material.SHULKER_BOX);
        var meta = shulker.getItemMeta();
        
        switch(type) {
            case 0:
                meta.setDisplayName("§5§lШалкер булавы");
                break;
            case 1:
                meta.setDisplayName("§c§lШалкер тотема");
                break;
            case 2:
                meta.setDisplayName("§6§lШалкер короны");
                break;
            case 3:
                meta.setDisplayName("§2§lШалкер карт");
                break;
        }
        
        shulker.setItemMeta(meta);
        player.getInventory().addItem(shulker);
    }
}