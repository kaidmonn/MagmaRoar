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
    private final Map<UUID, Integer> itemSlot = new HashMap<>();
    private final Random random = new Random();
    
    private static final long COOLDOWN_TIME = 35 * 1000;
    private static final int ACTIVE_TIME = 30;
    
    private final String[] ITEMS = {
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
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // ДИАГНОСТИКА
        player.sendMessage("§e[DEBUG] Действие: " + event.getAction());
        player.sendMessage("§e[DEBUG] Предмет: " + (item != null ? item.getType() : "null"));
        
        if (item == null) {
            player.sendMessage("§c[DEBUG] Предмета нет в руке");
            return;
        }
        
        if (item.getType() != Material.NETHERITE_SWORD) {
            player.sendMessage("§c[DEBUG] Это не незерит меч");
            return;
        }
        
        if (!item.hasItemMeta()) {
            player.sendMessage("§c[DEBUG] У предмета нет меты");
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        player.sendMessage("§e[DEBUG] Название: " + (meta.hasDisplayName() ? meta.getDisplayName() : "нет названия"));
        
        if (!isLudoSword(item)) {
            player.sendMessage("§c[DEBUG] Не Лудо-меч");
            return;
        }
        
        player.sendMessage("§a[DEBUG] Лудо-меч опознан!");
        
        // Только ПКМ
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            player.sendMessage("§c[DEBUG] Не ПКМ");
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_TIME) {
            long secondsLeft = (COOLDOWN_TIME - (now - cooldowns.get(uuid))) / 1000;
            player.sendMessage("§cЛудо-меч перезаряжается! Осталось: " + secondsLeft + " сек.");
            event.setCancelled(true);
            return;
        }
        
        if (isActive.getOrDefault(uuid, false)) {
            player.sendMessage("§cУ вас уже есть активный предмет!");
            event.setCancelled(true);
            return;
        }
        
        player.sendMessage("§a[DEBUG] Всё ок, начинаем рулетку!");
        
        itemSlot.put(uuid, player.getInventory().getHeldItemSlot());
        
        int index = random.nextInt(ITEMS.length);
        String selectedItem = ITEMS[index];
        String selectedName = ITEM_NAMES[index];
        
        player.sendMessage("§6§l═══════════════════════");
        player.sendMessage("§6§l  ВЫПАЛО: " + selectedName);
        player.sendMessage("§6§l═══════════════════════");
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        player.getInventory().setItemInMainHand(null);
        giveRandomItem(player, selectedItem, selectedName);
        
        cooldowns.put(uuid, now);
        isActive.put(uuid, true);
        
        event.setCancelled(true);
    }

    private void giveRandomItem(Player player, String itemName, String displayName) {
        String command = "";
        
        switch (itemName) {
            case "frost":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1005\"]}," +
                    "item_name='{\"text\":\"Морозный меч\",\"color\":\"aqua\",\"bold\":true}'" +
                    "] 1";
                break;
            case "shadow":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1006\"]}," +
                    "item_name='{\"text\":\"Теневой меч\",\"color\":\"dark_gray\",\"bold\":true}'" +
                    "] 1";
                break;
            case "spider":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1007\"]}," +
                    "item_name='{\"text\":\"Паучий клинок\",\"color\":\"dark_green\",\"bold\":true}'" +
                    "] 1";
                break;
            case "mjolnir":
                command = "give " + player.getName() + " minecraft:iron_axe[" +
                    "custom_model_data={strings:[\"1008\"]}," +
                    "item_name='{\"text\":\"Мьёльнир\",\"color\":\"yellow\",\"bold\":true}'" +
                    "] 1";
                break;
            case "scythe":
                command = "give " + player.getName() + " minecraft:netherite_hoe[" +
                    "custom_model_data={strings:[\"1009\"]}," +
                    "item_name='{\"text\":\"Коса смерти\",\"color\":\"red\",\"bold\":true}'" +
                    "] 1";
                break;
            case "storm":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1010\"]}," +
                    "item_name='{\"text\":\"Клинок бури\",\"color\":\"blue\",\"bold\":true}'" +
                    "] 1";
                break;
            case "reaper":
                command = "give " + player.getName() + " minecraft:netherite_hoe[" +
                    "custom_model_data={strings:[\"1011\"]}," +
                    "item_name='{\"text\":\"Коса жнеца\",\"color\":\"dark_purple\",\"bold\":true}'" +
                    "] 1";
                break;
            case "katana":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1012\"]}," +
                    "item_name='{\"text\":\"Катана дракона\",\"color\":\"light_purple\",\"bold\":true}'" +
                    "] 1";
                break;
            case "excalibur":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1013\"]}," +
                    "item_name='{\"text\":\"Экскалибур\",\"color\":\"gold\",\"bold\":true}'" +
                    "] 1";
                break;
            case "mace":
                command = "give " + player.getName() + " minecraft:mace[" +
                    "custom_model_data={strings:[\"1014\"]}," +
                    "item_name='{\"text\":\"Легкая булава\",\"color\":\"white\",\"bold\":true}'" +
                    "] 1";
                break;
            case "jackpot":
                command = "give " + player.getName() + " minecraft:netherite_sword[" +
                    "custom_model_data={strings:[\"1015\"]}," +
                    "item_name='{\"text\":\"ДЖЕКПОТ\",\"color\":\"light_purple\",\"bold\":true}'" +
                    "] 1";
                break;
        }
        
        if (!command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        
        startReturnTimer(player);
    }

    private void startReturnTimer(Player player) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                int slot = itemSlot.getOrDefault(uuid, 0);
                player.getInventory().setItem(slot, null);
                LudoSwordItem.giveLudoSword(player);
                player.sendMessage("§cВыпавший предмет исчез. Лудо-меч вернулся!");
                isActive.put(uuid, false);
                itemSlot.remove(uuid);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ACTIVE_TIME * 20L);
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && 
               meta.getDisplayName().contains("Лудо-меч");
    }
}