package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // ← ЭТОТ ИМПОРТ БЫЛ ПРОПУЩЕН!
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
        
        Inventory enderChest = templatePlayer.getEnderChest();
        
        // Выводим в консоль всё содержимое эндер-сундука для диагностики
        plugin.getLogger().info("§e[KitManager] Содержимое эндер-сундука " + TEMPLATE_PLAYER + ":");
        for (int i = 0; i < enderChest.getSize(); i++) {
            ItemStack item = enderChest.getItem(i);
            if (item != null && item.getType() == Material.SHULKER_BOX) {
                ItemMeta meta = item.getItemMeta();
                String name = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : "без названия";
                plugin.getLogger().info("§eСлот " + i + ": " + name);
            }
        }
        
        // Ищем базовый кит
        ItemStack basicKit = null;
        List<ItemStack> bonusKits = new ArrayList<>();
        
        for (ItemStack item : enderChest.getContents()) {
            if (item == null || item.getType() != Material.SHULKER_BOX) continue;
            
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            
            String name = ChatColor.stripColor(meta.getDisplayName()); // Убираем цветные коды для сравнения
            String rawName = meta.getDisplayName(); // Оригинальное название с цветами
            
            plugin.getLogger().info("§e[KitManager] Найден шалкер: " + rawName);
            
            if (name.contains("Базовый кит") || rawName.contains("Базовый кит")) {
                basicKit = item.clone();
                plugin.getLogger().info("§a[KitManager] Базовый кит найден!");
            } else if (name.contains("Шалкер") || rawName.contains("Шалкер")) {
                bonusKits.add(item.clone());
                plugin.getLogger().info("§a[KitManager] Бонусный шалкер найден: " + rawName);
            }
        }
        
        if (basicKit == null) {
            Bukkit.broadcastMessage("§c§lОШИБКА: Базовый кит не найден в эндер-сундуке!");
            plugin.getLogger().severe("[KitManager] Базовый кит не найден!");
            return;
        }
        
        plugin.getLogger().info("§a[KitManager] Найдено бонусных шалкеров: " + bonusKits.size());
        
        // Выдаём киты игрокам
        for (Player player : players) {
            // Очищаем инвентарь перед выдачей
            player.getInventory().clear();
            
            // Выдаём базовый кит
            player.getInventory().addItem(basicKit.clone());
            player.sendMessage("§aВы получили базовый кит!");
            
            // 50% шанс на бонусный кит
            if (!bonusKits.isEmpty()) {
                int chance = random.nextInt(100);
                plugin.getLogger().info("§e[KitManager] Шанс для " + player.getName() + ": " + chance + "%");
                
                if (chance < 50) {
                    ItemStack bonus = bonusKits.get(random.nextInt(bonusKits.size())).clone();
                    player.getInventory().addItem(bonus);
                    
                    ItemMeta meta = bonus.getItemMeta();
                    String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "бонусный шалкер";
                    player.sendMessage("§aВы получили " + name);
                    plugin.getLogger().info("§a[KitManager] " + player.getName() + " получил бонус: " + name);
                } else {
                    player.sendMessage("§eВам не повезло (50% шанс)");
                }
            } else {
                player.sendMessage("§cБонусные шалкеры не найдены в эндер-сундуке!");
            }
        }
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }
}