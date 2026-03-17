package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitManager {

    private final MagmaRoarPlugin plugin;
    private final Random random = new Random();
    private ItemManager itemManager;

    public KitManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    public void giveKits(List<Player> players) {
        for (Player player : players) {
            giveBasicKit(player);
            
            // 30% шанс на доп. предметы
            if (random.nextInt(100) < 30) {
                giveBonusItems(player);
            }
        }
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }

    private void giveBasicKit(Player player) {
        player.sendMessage("§aВыдача базового кита...");
        
        // Броня
        player.getInventory().setHelmet(itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_HELMET));
        player.getInventory().setChestplate(itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_CHESTPLATE));
        player.getInventory().setLeggings(itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_LEGGINGS));
        player.getInventory().setBoots(itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_BOOTS));
        
        // Меч и топор
        player.getInventory().addItem(itemManager.createUnbreakableNetheriteSword());
        player.getInventory().addItem(itemManager.createNetheriteAxe());
        
        // Кирка и щит
        player.getInventory().addItem(itemManager.createNetheritePickaxe());
        player.getInventory().addItem(itemManager.createShield());
        
        // Еда (золотые яблоки)
        ItemStack goldenApple = itemManager.createGoldenApple();
        goldenApple.setAmount(24);
        player.getInventory().addItem(goldenApple);
        
        // Зачарованное яблоко
        player.getInventory().addItem(itemManager.createEnchantedGoldenApple());
        
        // Эндер жемчуг (48 штук)
        ItemStack enderPearls = itemManager.createEnderPearl();
        enderPearls.setAmount(48);
        player.getInventory().addItem(enderPearls);
        
        // Заряды ветра (2 стака)
        ItemStack windCharges = itemManager.createWindCharge();
        windCharges.setAmount(64);
        player.getInventory().addItem(windCharges.clone());
        player.getInventory().addItem(windCharges.clone());
        
        // Паутина (1 стак)
        ItemStack cobweb = itemManager.createCobweb();
        cobweb.setAmount(64);
        player.getInventory().addItem(cobweb);
        
        // Ведро воды
        player.getInventory().addItem(itemManager.createWaterBucket());
        
        // Дубовые брёвна (1 стак)
        ItemStack logs = itemManager.createOakLogs();
        logs.setAmount(64);
        player.getInventory().addItem(logs);
        
        // Стрелы (2 стака)
        ItemStack arrows = itemManager.createArrows();
        arrows.setAmount(64);
        player.getInventory().addItem(arrows.clone());
        player.getInventory().addItem(arrows.clone());
        
        player.sendMessage("§a✓ Базовый кит выдан!");
    }

    private void giveBonusItems(Player player) {
        int type = random.nextInt(4);
        
        switch(type) {
            case 0: // Булава
                player.getInventory().addItem(itemManager.createMaceWithBreach());
                player.sendMessage("§5✓ Бонус: Булава пробития!");
                break;
                
            case 1: // Тотем
                player.getInventory().addItem(itemManager.createTotem());
                player.getInventory().addItem(itemManager.createEnchantedGoldenApple());
                player.getInventory().addItem(itemManager.createEnchantedGoldenApple());
                player.sendMessage("§c✓ Бонус: Тотем и яблоки!");
                break;
                
            case 2: // Корона
                player.getInventory().addItem(itemManager.createTotem());
                player.getInventory().addItem(itemManager.createTotem());
                player.getInventory().addItem(itemManager.createEnchantedGoldenApple());
                player.getInventory().addItem(itemManager.createEnchantedGoldenApple());
                player.getInventory().addItem(itemManager.createCrownHelmet());
                player.sendMessage("§6✓ Бонус: Корона владыки!");
                break;
                
            case 3: // Карты
                ItemStack tntMinecart = itemManager.createMinecartTNT();
                tntMinecart.setAmount(6);
                player.getInventory().addItem(tntMinecart);
                player.getInventory().addItem(itemManager.createFlameBow());
                
                ItemStack arrows = itemManager.createArrows();
                arrows.setAmount(64);
                player.getInventory().addItem(arrows);
                
                ItemStack rails = itemManager.createRails();
                rails.setAmount(64);
                player.getInventory().addItem(rails);
                
                player.sendMessage("§2✓ Бонус: Вагонетки и лук!");
                break;
        }
    }
}