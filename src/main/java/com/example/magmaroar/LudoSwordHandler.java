package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isActive = new HashMap<>();
    private final Set<UUID> rollingPlayers = new HashSet<>();
    private final Random random = new Random();
    
    // Ключ для пометки временного меча
    private final NamespacedKey tempKey = new NamespacedKey(MagmaRoarPlugin.getInstance(), "temp_sword");

    private static final long COOLDOWN_TIME = 35 * 1000; 
    private static final int ACTIVE_TIME = 30; // Секунд

    private final String[] ITEM_NAMES = {
        "§bМорозный меч", "§8Теневой меч", "§2Паучий клинок", "§eМьёльнир", 
        "§cКоса смерти", "§9Клинок бури", "§5Коса жнеца", "§dКатана дракона", 
        "§6Экскалибур", "§fЛегкая булава", "§d§lДЖЕКПОТ"
    };

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLudoSword(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (rollingPlayers.contains(uuid)) return;
        
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            player.sendMessage("§cПерезарядка: " + (cooldowns.get(uuid) - now) / 1000 + " сек.");
            return;
        }
        
        if (isActive.getOrDefault(uuid, false)) {
            player.sendMessage("§cУ вас уже есть активный предмет!");
            return;
        }
        
        startRoulette(player, item);
        event.setCancelled(true);
    }

    private void startRoulette(Player player, ItemStack ludoSword) {
        UUID uuid = player.getUniqueId();
        rollingPlayers.add(uuid);
        
        player.sendMessage("§6§l🔄 РУЛЕТКА ЗАПУЩЕНА...");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    rollingPlayers.remove(uuid);
                    
                    int index = random.nextInt(ITEM_NAMES.length);
                    String itemName = ITEM_NAMES[index];
                    
                    player.sendMessage("§6§l ВЫПАЛО: " + itemName);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    
                    // 1. Забираем Лудо-меч (уменьшаем количество на 1)
                    ludoSword.setAmount(ludoSword.getAmount() - 1);
                    
                    // 2. Создаем и выдаем временный меч
                    ItemStack reward = createTempSword(itemName);
                    player.getInventory().addItem(reward);
                    
                    isActive.put(uuid, true);
                    startReturnTimer(player);
                    
                    this.cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
                }
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void startReturnTimer(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                
                // Удаляем ВСЕ временные мечи из инвентаря
                for (ItemStack is : player.getInventory().getContents()) {
                    if (is != null && is.hasItemMeta()) {
                        if (is.getItemMeta().getPersistentDataContainer().has(tempKey, PersistentDataType.BYTE)) {
                            is.setAmount(0);
                        }
                    }
                }
                
                // Возвращаем основной Лудо-меч
                player.getInventory().addItem(LudoSwordItem.createSword());
                player.sendMessage("§cВремя вышло! Лудо-меч вернулся.");
                
                cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_TIME);
                isActive.put(uuid, false);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ACTIVE_TIME * 20L);
    }

    private ItemStack createTempSword(String name) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            // Помечаем предмет как временный
            meta.getPersistentDataContainer().set(tempKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Лудо");
    }
}