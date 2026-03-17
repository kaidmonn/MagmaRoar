package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class KitManager {

    private final MagmaRoarPlugin plugin;
    private final Random random = new Random();
    private final ItemManager itemManager;

    public KitManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    public void giveKits(List<Player> players) {
        Bukkit.broadcastMessage("§a[KitManager] giveKits вызван! Игроков: " + players.size());
        
        for (Player player : players) {
            Bukkit.broadcastMessage("§a[KitManager] Выдача кита игроку: " + player.getName());
            giveBasicKit(player);
            
            // 30% шанс на доп. предметы
            if (random.nextInt(100) < 30) {
                giveBonusItems(player);
            }
        }
        
        Bukkit.broadcastMessage("§a[KitManager] Выдача завершена, вызываем randomweapon команды");
        
        // Вызываем команды рандомного оружия
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall2");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "randomweaponall1");
    }

    private void giveBasicKit(Player player) {
        player.sendMessage("§aВыдача базового кита...");
        
        try {
            // Броня
            ItemStack helmet = itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_HELMET);
            ItemStack chestplate = itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_CHESTPLATE);
            ItemStack leggings = itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_LEGGINGS);
            ItemStack boots = itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_BOOTS);
            
            if (helmet == null) player.sendMessage("§c[DEBUG] helmet = null");
            else player.getInventory().setHelmet(helmet);
            
            if (chestplate == null) player.sendMessage("§c[DEBUG] chestplate = null");
            else player.getInventory().setChestplate(chestplate);
            
            if (leggings == null) player.sendMessage("§c[DEBUG] leggings = null");
            else player.getInventory().setLeggings(leggings);
            
            if (boots == null) player.sendMessage("§c[DEBUG] boots = null");
            else player.getInventory().setBoots(boots);
            
            // Меч и топор
            ItemStack sword = itemManager.createUnbreakableNetheriteSword();
            if (sword != null) player.getInventory().addItem(sword);
            
            ItemStack axe = itemManager.createNetheriteAxe();
            if (axe != null) player.getInventory().addItem(axe);
            
            // Кирка и щит
            ItemStack pickaxe = itemManager.createNetheritePickaxe();
            if (pickaxe != null) player.getInventory().addItem(pickaxe);
            
            ItemStack shield = itemManager.createShield();
            if (shield != null) player.getInventory().addItem(shield);
            
            // Еда (золотые яблоки)
            ItemStack goldenApple = itemManager.createGoldenApple();
            if (goldenApple != null) {
                goldenApple.setAmount(24);
                player.getInventory().addItem(goldenApple);
            }
            
            // Зачарованное яблоко
            ItemStack enchantedApple = itemManager.createEnchantedGoldenApple();
            if (enchantedApple != null) player.getInventory().addItem(enchantedApple);
            
            // Эндер жемчуг (48 штук)
            ItemStack enderPearls = itemManager.createEnderPearl();
            if (enderPearls != null) {
                enderPearls.setAmount(48);
                player.getInventory().addItem(enderPearls);
            }
            
            // Заряды ветра (2 стака)
            ItemStack windCharges = itemManager.createWindCharge();
            if (windCharges != null) {
                windCharges.setAmount(64);
                player.getInventory().addItem(windCharges.clone());
                player.getInventory().addItem(windCharges.clone());
            }
            
            // Паутина (1 стак)
            ItemStack cobweb = itemManager.createCobweb();
            if (cobweb != null) {
                cobweb.setAmount(64);
                player.getInventory().addItem(cobweb);
            }
            
            // Ведро воды
            ItemStack waterBucket = itemManager.createWaterBucket();
            if (waterBucket != null) player.getInventory().addItem(waterBucket);
            
            // Дубовые брёвна (1 стак)
            ItemStack logs = itemManager.createOakLogs();
            if (logs != null) {
                logs.setAmount(64);
                player.getInventory().addItem(logs);
            }
            
            // Стрелы (2 стака)
            ItemStack arrows = itemManager.createArrows();
            if (arrows != null) {
                arrows.setAmount(64);
                player.getInventory().addItem(arrows.clone());
                player.getInventory().addItem(arrows.clone());
            }
            
            player.sendMessage("§a✓ Базовый кит выдан!");
            
        } catch (Exception e) {
            player.sendMessage("§cОшибка при выдаче кита: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void giveBonusItems(Player player) {
        int type = random.nextInt(4);
        
        try {
            switch(type) {
                case 0: // Булава
                    ItemStack mace = itemManager.createMaceWithBreach();
                    if (mace != null) {
                        player.getInventory().addItem(mace);
                        player.sendMessage("§5✓ Бонус: Булава пробития!");
                    }
                    break;
                    
                case 1: // Тотем
                    ItemStack totem = itemManager.createTotem();
                    ItemStack apple = itemManager.createEnchantedGoldenApple();
                    if (totem != null) player.getInventory().addItem(totem);
                    if (apple != null) {
                        player.getInventory().addItem(apple.clone());
                        player.getInventory().addItem(apple.clone());
                    }
                    player.sendMessage("§c✓ Бонус: Тотем и яблоки!");
                    break;
                    
                case 2: // Корона
                    ItemStack totem1 = itemManager.createTotem();
                    ItemStack totem2 = itemManager.createTotem();
                    ItemStack apple1 = itemManager.createEnchantedGoldenApple();
                    ItemStack apple2 = itemManager.createEnchantedGoldenApple();
                    ItemStack crown = itemManager.createCrownHelmet();
                    
                    if (totem1 != null) player.getInventory().addItem(totem1);
                    if (totem2 != null) player.getInventory().addItem(totem2);
                    if (apple1 != null) player.getInventory().addItem(apple1);
                    if (apple2 != null) player.getInventory().addItem(apple2);
                    if (crown != null) player.getInventory().addItem(crown);
                    
                    player.sendMessage("§6✓ Бонус: Корона владыки!");
                    break;
                    
                case 3: // Карты
                    ItemStack tntMinecart = itemManager.createMinecartTNT();
                    if (tntMinecart != null) {
                        tntMinecart.setAmount(6);
                        player.getInventory().addItem(tntMinecart);
                    }
                    
                    ItemStack flameBow = itemManager.createFlameBow();
                    if (flameBow != null) player.getInventory().addItem(flameBow);
                    
                    ItemStack arrows = itemManager.createArrows();
                    if (arrows != null) {
                        arrows.setAmount(64);
                        player.getInventory().addItem(arrows);
                    }
                    
                    ItemStack rails = itemManager.createRails();
                    if (rails != null) {
                        rails.setAmount(64);
                        player.getInventory().addItem(rails);
                    }
                    
                    player.sendMessage("§2✓ Бонус: Вагонетки и лук!");
                    break;
            }
        } catch (Exception e) {
            player.sendMessage("§cОшибка при выдаче бонуса: " + e.getMessage());
            e.printStackTrace();
        }
    }
}