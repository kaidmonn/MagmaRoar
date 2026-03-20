package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isActive = new HashMap<>();
    private final Map<UUID, Integer> itemSlot = new HashMap<>();
    private final Set<UUID> rollingPlayers = new HashSet<>();
    private final Set<UUID> lockedSlots = new HashSet<>();
    private final Random random = new Random();
    
    private static final long COOLDOWN_TIME = 35 * 1000;
    private static final int ACTIVE_TIME = 30;
    
    private final String[] COMMANDS = {
        "frost", "shadow", "spider", "mjolnir", "scythe",
        "storm", "reaper", "katana", "excalibur", "mace",
        "jackpot"
    };
    
    private final String[] ITEM_NAMES = {
        "§bМорозный меч",
        "§8Теневой меч",
        "§2Паучий клинок",
        "§eМьёльнир",
        "§cКоса смерти",
        "§9Клинок бури",
        "§5Коса жнеца",
        "§dКатана дракона",
        "§6Экскалибур",
        "§fЛегкая булава",
        "§d§lДЖЕКПОТ"
    };

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLudoSword(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (rollingPlayers.contains(uuid)) {
            player.sendMessage("§cРулетка уже крутится!");
            event.setCancelled(true);
            return;
        }
        
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long secondsLeft = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cЛудо-меч перезаряжается! Осталось: " + secondsLeft + " сек.");
            event.setCancelled(true);
            return;
        }
        
        if (isActive.getOrDefault(uuid, false)) {
            player.sendMessage("§cУ вас уже есть активный предмет!");
            event.setCancelled(true);
            return;
        }
        
        int slot = player.getInventory().getHeldItemSlot();
        itemSlot.put(uuid, slot);
        lockedSlots.add(uuid);
        
        startRoulette(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (lockedSlots.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void startRoulette(Player player) {
        UUID uuid = player.getUniqueId();
        rollingPlayers.add(uuid);
        
        player.sendMessage("§6§l🔄 ЛУДО-МЕЧ: КРУТИТСЯ РУЛЕТКА...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    
                    rollingPlayers.remove(uuid);
                    
                    int index = random.nextInt(COMMANDS.length);
                    String command = COMMANDS[index];
                    String itemName = ITEM_NAMES[index];
                    
                    player.sendMessage("§6§l═══════════════════════");
                    player.sendMessage("§6§l  ВЫПАЛО: " + itemName);
                    player.sendMessage("§6§l═══════════════════════");
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    
                    // Удаляем Лудо-меч
                    player.getInventory().setItemInMainHand(null);
                    
                    // ВЫЗЫВАЕМ КОМАНДУ ПРЕДМЕТА
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command + " " + player.getName());
                    
                    isActive.put(uuid, true);
                    lockedSlots.remove(uuid);
                    
                    // Запускаем таймер возврата
                    startReturnTimer(player);
                    
                    this.cancel();
                    return;
                }
                
                if (ticks % 4 == 0) {
                    int randomIndex = random.nextInt(COMMANDS.length);
                    player.sendMessage("§8> " + ITEM_NAMES[randomIndex]);
                }
                
                if (ticks % 8 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void startReturnTimer(Player player) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                int slot = itemSlot.getOrDefault(uuid, 0);
                
                // Очищаем слот
                player.getInventory().setItem(slot, null);
                
                // Возвращаем Лудо-меч
                LudoSwordItem.giveLudoSword(player);
                
                player.sendMessage("§cВыпавший предмет исчез. Лудо-меч вернулся!");
                
                cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_TIME);
                isActive.put(uuid, false);
                itemSlot.remove(uuid);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ACTIVE_TIME * 20L);
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.contains("Лудо-меч") || name.contains("Лудо");
    }
}