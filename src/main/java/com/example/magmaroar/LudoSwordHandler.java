package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isActive = new HashMap<>();
    private final Set<UUID> rollingPlayers = new HashSet<>();
    private final Random random = new Random();

    private static final long COOLDOWN_TIME = 35 * 1000; 
    private static final int ACTIVE_TIME = 30; // 30 секунд действия

    // Список команд (без слеша, так как ConsoleSender его не требует)
    private final String[] COMMANDS = {
        "frost", "shadow", "spider", "mjolnir", "scythe",
        "storm", "reaper", "katana", "excalibur", "mace", "jackpot"
    };
    
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
            player.sendMessage("§cЛудо-меч перезаряжается! Осталось: " + (cooldowns.get(uuid) - now) / 1000 + " сек.");
            return;
        }
        
        if (isActive.getOrDefault(uuid, false)) {
            player.sendMessage("§cУ вас уже есть активный предмет!");
            return;
        }
        
        startRoulette(player, item);
        event.setCancelled(true);
    }

    private void startRoulette(Player player, ItemStack ludoItem) {
        UUID uuid = player.getUniqueId();
        rollingPlayers.add(uuid);
        
        player.sendMessage("§6§l🔄 ЛУДО-МЕЧ: КРУТИТСЯ РУЛЕТКА...");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    rollingPlayers.remove(uuid);
                    
                    int index = random.nextInt(COMMANDS.length);
                    String command = COMMANDS[index];
                    String itemName = ITEM_NAMES[index];
                    
                    player.sendMessage("§6§l ВЫПАЛО: " + itemName);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    
                    // 1. УДАЛЯЕМ ЛУДО-МЕЧ ИЗ РУКИ ПЕРЕД ВЫДАЧЕЙ
                    if (ludoItem.getAmount() > 1) {
                        ludoItem.setAmount(ludoItem.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                    
                    // 2. ВЫПОЛНЯЕМ КОМАНДУ (Убедитесь, что команда существует!)
                    // Формат: "mace NickName"
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command + " " + player.getName());
                    
                    isActive.put(uuid, true);
                    startReturnTimer(player, itemName); // Передаем имя, чтобы найти и удалить потом
                    
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

    private void startReturnTimer(Player player, String itemName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Ищем в инвентаре предмет с таким же именем, какое было у выпавшего
                for (ItemStack stack : player.getInventory().getContents()) {
                    if (stack != null && stack.hasItemMeta()) {
                        if (stack.getItemMeta().getDisplayName().equals(itemName)) {
                            stack.setAmount(0); // Удаляем
                        }
                    }
                }
                
                // Возвращаем Лудо-меч
                player.getInventory().addItem(LudoSwordItem.createSword());
                player.sendMessage("§cВременный предмет исчез. Лудо-меч вернулся!");
                
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_TIME);
                isActive.put(player.getUniqueId(), false);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ACTIVE_TIME * 20L);
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name != null && (name.contains("Лудо-меч") || name.contains("Лудо"));
    }
}