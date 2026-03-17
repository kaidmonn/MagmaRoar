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
            Bukkit.broadcastMessage("§c§lИгрок " + TEMPLATE_PLAYER + " не в сети! Бонусы не выданы.");
            return;
        }
        
        Inventory enderChest = templatePlayer.getEnderChest();
        
        for (Player player : players) {
            // 50% шанс на бонус
            if (random.nextInt(100) < 50) {
                giveRandomBonusShulker(player, enderChest);
            }
        }
    }

    private void giveRandomBonusShulker(Player player, Inventory enderChest) {
        List<ItemStack> bonusShulkers = new ArrayList<>();
        
        // Собираем все шалкеры из эндер-сундука
        for (ItemStack item : enderChest.getContents()) {
            if (item != null && item.getType().name().contains("SHULKER_BOX")) {
                bonusShulkers.add(item);
            }
        }
        
        if (bonusShulkers.isEmpty()) {
            player.sendMessage("§cВ эндер-сундуке нет шалкеров!");
            return;
        }
        
        // Выбираем случайный
        ItemStack randomBonus = bonusShulkers.get(random.nextInt(bonusShulkers.size())).clone();
        player.getInventory().addItem(randomBonus);
        
        // Название для красоты
        ItemMeta meta = randomBonus.getItemMeta();
        String name = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : "бонусный шалкер";
        player.sendMessage("§aВы получили " + name);
    }
}