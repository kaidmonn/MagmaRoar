package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final MagmaRoarPlugin plugin = MagmaRoarPlugin.getInstance();
    private final Random random = new Random();
    
    // Ключи для базы данных предмета
    private final NamespacedKey KEY_LUDO = new NamespacedKey(plugin, "is_ludo_main");
    private final NamespacedKey KEY_TEMP = new NamespacedKey(plugin, "is_ludo_temp");

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> rolling = new HashSet<>();

    // Массивы команд и имен для вывода в чат
    private final String[] COMMANDS = {"frost", "shadow", "spider", "mjolnir", "scythe", "mace", "jackpot"};
    private final String[] NAMES = {"§bМорозный меч", "§8Теневой меч", "§2Паучий клинок", "§eМьёльнир", "§cКоса", "§fБулава", "§d§lДЖЕКПОТ"};

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = e.getItem();
        if (item == null || !isMainLudo(item)) return;

        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Проверки
        if (rolling.contains(id)) return;
        if (hasTempItem(p)) {
            p.sendMessage("§cУ вас уже есть активное оружие!");
            return;
        }
        if (cooldowns.getOrDefault(id, 0L) > System.currentTimeMillis()) {
            long left = (cooldowns.get(id) - System.currentTimeMillis()) / 1000;
            p.sendMessage("§cПерезарядка: " + left + " сек.");
            return;
        }

        startRoulette(p, item);
        e.setCancelled(true);
    }

    private void startRoulette(Player p, ItemStack ludo) {
        rolling.add(p.getUniqueId());
        p.sendMessage("§6§l🔄 Испытываем удачу...");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    rolling.remove(p.getUniqueId());
                    int res = random.nextInt(COMMANDS.length);
                    
                    // 1. Забираем Лудо-меч
                    ludo.setAmount(ludo.getAmount() - 1);
                    
                    // 2. Выдаем предмет через команду
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), COMMANDS[res] + " " + p.getName());
                    
                    // 3. Через 1 тик помечаем выданный предмет как временный
                    Bukkit.getScheduler().runTask(plugin, () -> markTempItem(p));

                    p.sendMessage("§6§lВЫПАЛО: " + NAMES[res]);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                    // 4. Таймер возврата (30 сек действие + выдача нового Лудо)
                    startReturnTimer(p);
                    
                    this.cancel();
                    return;
                }
                if (ticks % 5 == 0) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startReturnTimer(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                removeTempItems(p);
                // Выдаем новый Лудо-меч (используйте ваш метод создания предмета)
                p.getInventory().addItem(createLudoSword());
                p.sendMessage("§cВремя вышло! Лудо-меч вернулся.");
                cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + 35000);
            }
        }.runTaskLater(plugin, 30 * 20L);
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (ЛОГИКА НАДЕЖНОСТИ) ---

    private void markTempItem(Player p) {
        // Помечаем первый найденный предмет без метки (который только что дала команда)
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() != Material.AIR && !isMainLudo(is)) {
                ItemMeta m = is.getItemMeta();
                if (!m.getPersistentDataContainer().has(KEY_TEMP, PersistentDataType.BYTE)) {
                    m.getPersistentDataContainer().set(KEY_TEMP, PersistentDataType.BYTE, (byte) 1);
                    is.setItemMeta(m);
                    return; 
                }
            }
        }
    }

    private void removeTempItems(Player p) {
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.hasItemMeta() && 
                is.getItemMeta().getPersistentDataContainer().has(KEY_TEMP, PersistentDataType.BYTE)) {
                is.setAmount(0);
            }
        }
    }

    private boolean hasTempItem(Player p) {
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.hasItemMeta() && 
                is.getItemMeta().getPersistentDataContainer().has(KEY_TEMP, PersistentDataType.BYTE)) return true;
        }
        return false;
    }

    private boolean isMainLudo(ItemStack is) {
        if (is == null || !is.hasItemMeta()) return false;
        return is.getItemMeta().getPersistentDataContainer().has(KEY_LUDO, PersistentDataType.BYTE);
    }

    // Метод для создания самого Лудо-меча (вызывайте его при выдаче игроку изначально)
    public ItemStack createLudoSword() {
        ItemStack stack = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = stack.getItemMeta();
        m.setDisplayName("§6§lЛудо-меч");
        m.getPersistentDataContainer().set(KEY_LUDO, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(m);
        return stack;
    }

    // --- ЗАЩИТА ---

    @EventHandler // Запрет выбрасывания
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack is = e.getItemDrop().getItemStack();
        if (is.hasItemMeta() && is.getItemMeta().getPersistentDataContainer().has(KEY_TEMP, PersistentDataType.BYTE)) {
            e.setCancelled(true);