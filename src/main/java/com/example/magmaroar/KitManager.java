package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;

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
        
        Inventory enderChest = templatePlayer.getEnderChest();
        
        // Ищем базовый кит
        ItemStack basicKit = null;
        List<ItemStack> bonusKits = new ArrayList<>();
        
        for (ItemStack item : enderChest.getContents()) {
            if (item == null || item.getType() != Material.SHULKER_BOX) continue;
            
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            
            String name = meta.getDisplayName();
            
            if (name.contains("Базовый кит")) {
                basicKit = item.clone();
            } else if (name.contains("Шалкер")) {
                bonusKits.add(item.clone());
            }
        }
        
        if (basicKit == null) {
            Bukkit.broadcastMessage("§c§lОШИБКА: Базовый кит не найден в эндер-сундуке!");
            return;
        }
        
        // Выдаём киты игрокам
        for (Player player : players) {
            // Очищаем инвентарь перед выдачей
            player.getInventory().clear();
            
            // Выдаём базовый кит
            player.getInventory().addItem(basicKit.clone());
            player.sendMessage("§aВы получили базовый кит!");
            
            // 50% шанс на бонусный кит
            if (!bonusKits.isEmpty() && random.nextInt(100) < 50) {
                ItemStack bonus = bonusKits.get(random.nextInt(bonusKits.size())).clone();
                player.getInventory().addItem(bonus);
                
                ItemMeta meta = bonus.getItemMeta();
                String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "бонусный шалкер";
                player.sendMessage("§aВы получили " + name);
            }
        }
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }
}