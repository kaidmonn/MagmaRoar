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
    private final String TEMPLATE_PLAYER = "kaidmonngrief"; // Игрок с заготовленными шалкерами

    public KitManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveKits(List<Player> players) {
        // Получаем эндер-сундук шаблонного игрока
        Player templatePlayer = Bukkit.getPlayer(TEMPLATE_PLAYER);
        if (templatePlayer == null || !templatePlayer.isOnline()) {
            Bukkit.broadcastMessage("§c§lОШИБКА: Игрок " + TEMPLATE_PLAYER + " не в сети! Киты не выданы.");
            return;
        }
        
        Inventory templateEnderChest = templatePlayer.getEnderChest();
        
        // Проверяем, есть ли там шалкеры
        boolean hasBasicShulker = false;
        boolean hasBonusShulkers = false;
        
        for (ItemStack item : templateEnderChest.getContents()) {
            if (item != null && item.getType() == Material.SHULKER_BOX) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName() != null) {
                    if (meta.getDisplayName().contains("Базовый кит")) {
                        hasBasicShulker = true;
                    } else if (meta.getDisplayName().contains("Шалкер")) {
                        hasBonusShulkers = true;
                    }
                }
            }
        }
        
        if (!hasBasicShulker) {
            Bukkit.broadcastMessage("§c§lОШИБКА: В эндер-сундуке " + TEMPLATE_PLAYER + " нет шалкера 'Базовый кит'!");
            return;
        }
        
        // Выдаём киты игрокам
        for (Player player : players) {
            giveBasicKit(player, templateEnderChest);
            
            // 30% шанс на доп. шалкер
            if (random.nextInt(100) < 30) {
                giveBonusShulker(player, templateEnderChest);
            }
        }
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }

    private void giveBasicKit(Player player, Inventory templateEnderChest) {
        // Ищем шалкер "Базовый кит"
        for (ItemStack item : templateEnderChest.getContents()) {
            if (item != null && item.getType() == Material.SHULKER_BOX) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName() != null && meta.getDisplayName().contains("Базовый кит")) {
                    // Копируем шалкер
                    ItemStack kitShulker = item.clone();
                    player.getInventory().addItem(kitShulker);
                    player.sendMessage("§aВы получили базовый кит!");
                    return;
                }
            }
        }
        
        player.sendMessage("§cОшибка: Базовый кит не найден в эндер-сундуке!");
    }

    private void giveBonusShulker(Player player, Inventory templateEnderChest) {
        // Собираем все бонусные шалкеры
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
            player.sendMessage("§cБонусных шалкеров нет в эндер-сундуке!");
            return;
        }
        
        // Выбираем случайный
        ItemStack randomBonus = bonusShulkers.get(random.nextInt(bonusShulkers.size())).clone();
        player.getInventory().addItem(randomBonus);
        
        // Отправляем сообщение в зависимости от типа
        ItemMeta meta = randomBonus.getItemMeta();
        if (meta != null && meta.getDisplayName() != null) {
            player.sendMessage("§aВы получили " + meta.getDisplayName());
        } else {
            player.sendMessage("§aВы получили бонусный шалкер!");
        }
    }
}