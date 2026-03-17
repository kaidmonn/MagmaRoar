package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class KitManager {

    private final MagmaRoarPlugin plugin;
    private final Random random = new Random();
    private final String TEMPLATE_PLAYER = "kaidmonngrief";

    public KitManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveKits(List<Player> players) {
        Player templatePlayer = Bukkit.getPlayer(TEMPLATE_PLAYER);
        if (templatePlayer == null || !templatePlayer.isOnline()) {
            Bukkit.broadcastMessage("§c§lОШИБКА: Игрок " + TEMPLATE_PLAYER + " не в сети! Киты не выданы.");
            return;
        }
        
        Inventory templateEnderChest = templatePlayer.getEnderChest();
        
        for (Player player : players) {
            giveBasicKit(player, templateEnderChest);
            
            // 50% шанс на доп. шалкер
            if (random.nextInt(100) < 50) {
                giveBonusShulker(player, templateEnderChest);
            }
        }
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }

    private void giveBasicKit(Player player, Inventory templateEnderChest) {
        for (ItemStack item : templateEnderChest.getContents()) {
            if (item != null && item.getType() == Material.SHULKER_BOX) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName() != null && meta.getDisplayName().contains("Базовый кит")) {
                    ItemStack kitShulker = item.clone();
                    player.getInventory().addItem(kitShulker);
                    player.sendMessage("§aВы получили базовый кит!");
                    return;
                }
            }
        }
        player.sendMessage("§cОшибка: Базовый кит не найден!");
    }

    private void giveBonusShulker(Player player, Inventory templateEnderChest) {
        List<ItemStack> bonusShulkers = new ArrayList<>();
        
        for (ItemStack item : templateEnderChest.getContents()) {
            if (item != null && item.getType() == Material.SHULKER_BOX) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName() != null && 
                    meta.getDisplayName().contains("Шалкер") && 
                    !meta.getDisplayName().contains("Базовый")) {
                    bonusShulkers.add(item);
                }
            }
        }
        
        if (bonusShulkers.isEmpty()) {
            return;
        }
        
        ItemStack randomBonus = bonusShulkers.get(random.nextInt(bonusShulkers.size())).clone();
        player.getInventory().addItem(randomBonus);
        
        ItemMeta meta = randomBonus.getItemMeta();
        if (meta != null && meta.getDisplayName() != null) {
            player.sendMessage("§aВы получили " + meta.getDisplayName());
        }
    }
}