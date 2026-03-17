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
    private final ItemManager itemManager;

    public KitManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
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
        // Создаём шалкер
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = shulker.getItemMeta();
        meta.setDisplayName("§6§lБазовый кит");
        shulker.setItemMeta(meta);
        
        // Создаём инвентарь шалкера (27 слотов)
        Inventory shulkerInv = Bukkit.createInventory(null, 27, "Базовый кит");
        
        // ========== ЗАПОЛНЯЕМ ШАЛКЕР ==========
        
        // Броня (4 слота)
        shulkerInv.setItem(0, itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_HELMET));
        shulkerInv.setItem(1, itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_CHESTPLATE));
        shulkerInv.setItem(2, itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_LEGGINGS));
        shulkerInv.setItem(3, itemManager.createUnbreakableNetheriteArmor(Material.NETHERITE_BOOTS));
        
        // Меч и топор
        shulkerInv.setItem(4, itemManager.createUnbreakableNetheriteSword());
        shulkerInv.setItem(5, itemManager.createNetheriteAxe());
        
        // Кирка и щит
        shulkerInv.setItem(6, itemManager.createNetheritePickaxe());
        shulkerInv.setItem(7, itemManager.createShield());
        
        // Еда (золотые яблоки)
        ItemStack goldenApple = itemManager.createGoldenApple();
        goldenApple.setAmount(24); // полтора стака
        shulkerInv.setItem(8, goldenApple);
        
        // Зачарованное яблоко
        shulkerInv.setItem(9, itemManager.createEnchantedGoldenApple());
        
        // Эндер жемчуг (48 штук)
        ItemStack enderPearls = itemManager.createEnderPearl();
        enderPearls.setAmount(48);
        shulkerInv.setItem(10, enderPearls);
        
        // Заряды ветра (2 стака = 64 + 64)
        ItemStack windCharges = itemManager.createWindCharge();
        windCharges.setAmount(64);
        shulkerInv.setItem(11, windCharges.clone());
        shulkerInv.setItem(12, windCharges.clone());
        
        // Паутина (1 стак)
        ItemStack cobweb = itemManager.createCobweb();
        cobweb.setAmount(64);
        shulkerInv.setItem(13, cobweb);
        
        // Ведро воды
        shulkerInv.setItem(14, itemManager.createWaterBucket());
        
        // Дубовые брёвна (1 стак)
        ItemStack logs = itemManager.createOakLogs();
        logs.setAmount(64);
        shulkerInv.setItem(15, logs);
        
        // Стрелы (2 стака)
        ItemStack arrows = itemManager.createArrows();
        arrows.setAmount(64);
        shulkerInv.setItem(16, arrows.clone());
        shulkerInv.setItem(17, arrows.clone());
        
        // Сохраняем инвентарь в шалкер
        if (shulker.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
            org.bukkit.inventory.meta.BlockStateMeta blockMeta = (org.bukkit.inventory.meta.BlockStateMeta) shulker.getItemMeta();
            if (blockMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                org.bukkit.block.ShulkerBox box = (org.bukkit.block.ShulkerBox) blockMeta.getBlockState();
                box.getInventory().setContents(shulkerInv.getContents());
                blockMeta.setBlockState(box);
                shulker.setItemMeta(blockMeta);
            }
        }
        
        // Выдаём игроку
        player.getInventory().addItem(shulker);
        player.sendMessage("§aВы получили базовый кит!");
    }

    private void giveBonusShulker(Player player) {
        int type = random.nextInt(4);
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = shulker.getItemMeta();
        
        Inventory shulkerInv = Bukkit.createInventory(null, 27, "Бонусный кит");
        
        switch(type) {
            case 0: // Шалкер булавы
                meta.setDisplayName("§5§lШалкер булавы");
                shulker.setItemMeta(meta);
                
                shulkerInv.setItem(13, itemManager.createMaceWithBreach());
                break;
                
            case 1: // Шалкер тотема
                meta.setDisplayName("§c§lШалкер тотема");
                shulker.setItemMeta(meta);
                
                shulkerInv.setItem(12, itemManager.createTotem());
                shulkerInv.setItem(14, itemManager.createEnchantedGoldenApple());
                shulkerInv.setItem(14, itemManager.createEnchantedGoldenApple()); // второе яблоко
                break;
                
            case 2: // Шалкер короны
                meta.setDisplayName("§6§lШалкер короны");
                shulker.setItemMeta(meta);
                
                shulkerInv.setItem(11, itemManager.createTotem());
                shulkerInv.setItem(12, itemManager.createTotem()); // второй тотем
                shulkerInv.setItem(13, itemManager.createEnchantedGoldenApple());
                shulkerInv.setItem(14, itemManager.createEnchantedGoldenApple()); // второе яблоко
                shulkerInv.setItem(15, itemManager.createCrownHelmet());
                break;
                
            case 3: // Шалкер карт
                meta.setDisplayName("§2§lШалкер карт");
                shulker.setItemMeta(meta);
                
                // 6 вагонеток с тнт
                ItemStack tntMinecart = itemManager.createMinecartTNT();
                tntMinecart.setAmount(6);
                shulkerInv.setItem(11, tntMinecart);
                
                shulkerInv.setItem(12, itemManager.createFlameBow());
                
                ItemStack arrows = itemManager.createArrows();
                arrows.setAmount(64);
                shulkerInv.setItem(13, arrows);
                
                ItemStack rails = itemManager.createRails();
                rails.setAmount(64);
                shulkerInv.setItem(14, rails);
                break;
        }
        
        // Сохраняем инвентарь в шалкер
        if (shulker.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
            org.bukkit.inventory.meta.BlockStateMeta blockMeta = (org.bukkit.inventory.meta.BlockStateMeta) shulker.getItemMeta();
            if (blockMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                org.bukkit.block.ShulkerBox box = (org.bukkit.block.ShulkerBox) blockMeta.getBlockState();
                box.getInventory().setContents(shulkerInv.getContents());
                blockMeta.setBlockState(box);
                shulker.setItemMeta(blockMeta);
            }
        }
        
        player.getInventory().addItem(shulker);
        player.sendMessage("§aВы получили бонусный шалкер!");
    }
}