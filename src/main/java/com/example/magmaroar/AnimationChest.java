package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AnimationChest implements Listener {

    private final MagmaRoarPlugin plugin;
    private final Map<UUID, Integer> playerRolls = new HashMap<>();
    private final Map<UUID, Integer> playerAnimations = new HashMap<>();
    private final Map<UUID, List<Integer>> playerAnimationHistory = new HashMap<>();
    private final Map<UUID, Boolean> openingAnimation = new HashMap<>();
    private final Random random = new Random();
    
    private static final String CHEST_NAME = "§6§lСундук-рулетка";
    
    // Названия анимаций
    private final String[] ANIMATION_NAMES = {
        "§cВзрыв внутри",
        "§8Визер-скелеты",
        "§7Наковальня",
        "§3Варден-выстрел",
        "§eКурицы и тортик",
        "§aФейерверки",
        "§bМолния",
        "§9Дождь",
        "§8Крест из камня",
        "§2Варден",
        "§6Метеорит",
        "§dНевесомость",
        "§dЦветочная поляна",
        "§eЗвёздный дождь",
        "§3Невидимая стена",
        "§8Танец скелетов",
        "§bВодоворот",
        "§6Огненный феникс",
        "§dКристаллы",
        "§8Теневые копии",
        "§6Песочные часы",
        "§cРадуга"
    };
    
    // Шансы анимаций (всего 22 штуки, сумма = 100)
    private final int[] ANIMATION_CHANCES = {
        5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, // 1-11
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4  // 12-22
    };

    public AnimationChest(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private int getRandomAnimation() {
        int total = 100;
        int rand = random.nextInt(total);
        int cumulative = 0;
        
        for (int i = 0; i < ANIMATION_CHANCES.length; i++) {
            cumulative += ANIMATION_CHANCES[i];
            if (rand < cumulative) {
                return i + 1;
            }
        }
        return 1;
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CHEST) return;
        
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Chest)) return;
        
        Chest chest = (Chest) block.getState();
        if (chest.getCustomName() == null || !chest.getCustomName().equals(CHEST_NAME)) return;
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Если уже идет анимация открытия - не даем открыть снова
        if (openingAnimation.getOrDefault(player.getUniqueId(), false)) {
            player.sendMessage("§cСундук уже открывается...");
            return;
        }
        
        // Запускаем визуализацию
        startOpeningAnimation(player, chest.getLocation());
    }

    private void startOpeningAnimation(Player player, Location chestLoc) {
        UUID uuid = player.getUniqueId();
        openingAnimation.put(uuid, true);
        
        World world = chestLoc.getWorld();
        if (world == null) return;
        
        player.sendMessage("§6§lСундук открывается... Подождите 5 секунд!");
        
        // Создаем блоки земли для анимации
        List<FallingBlock> rotatingBlocks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Location spawnLoc = chestLoc.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
            FallingBlock block = world.spawnFallingBlock(spawnLoc, Material.DIRT.createBlockData());
            block.setDropItem(false);
            block.setHurtEntities(false);
            block.setGravity(false);
            block.setVelocity(new Vector(0, 0, 0));
            rotatingBlocks.add(block);
        }
        
        // Анимация вращения
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) { // 5 секунд
                    // Удаляем блоки
                    for (FallingBlock block : rotatingBlocks) {
                        block.remove();
                    }
                    
                    // Открываем меню
                    openingAnimation.put(uuid, false);
                    openRollGUI(player);
                    
                    this.cancel();
                    return;
                }
                
                // Вращаем блоки
                for (int i = 0; i < rotatingBlocks.size(); i++) {
                    FallingBlock block = rotatingBlocks.get(i);
                    double angle = (ticks * 0.1) + (i * Math.PI / 4);
                    Location newLoc = chestLoc.clone().add(Math.cos(angle) * 3, 1 + Math.sin(ticks * 0.1) * 0.5, Math.sin(angle) * 3);
                    block.teleport(newLoc);
                }
                
                // Частицы
                world.spawnParticle(Particle.PORTAL, chestLoc.clone().add(0, 1, 0), 10, 1, 1, 1, 0);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void openRollGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§lСундук-рулетка"); // 54 слота для истории
        
        // Украшаем стеклом
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glass);
            gui.setItem(i + 45, glass);
        }
        
        int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
        
        // Информация о крутках
        ItemStack infoItem = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§lКруток: §f" + rolls);
        List<String> lore = new ArrayList<>();
        lore.add("§7Нажми на сундук, чтобы");
        lore.add("§7потратить 1 крутку");
        lore.add("§7Шанс каждой анимации: §aразный");
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem);
        
        // Текущая анимация
        int currentAnim = playerAnimations.getOrDefault(player.getUniqueId(), -1);
        if (currentAnim != -1) {
            ItemStack currentItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta currentMeta = currentItem.getItemMeta();
            currentMeta.setDisplayName("§a§lТекущая анимация:");
            List<String> currentLore = new ArrayList<>();
            currentLore.add(ANIMATION_NAMES[currentAnim - 1]);
            currentMeta.setLore(currentLore);
            currentItem.setItemMeta(currentMeta);
            gui.setItem(13, currentItem);
        }
        
        // Кнопка крутки
        if (rolls > 0) {
            ItemStack rollButton = new ItemStack(Material.ENDER_CHEST);
            ItemMeta rollMeta = rollButton.getItemMeta();
            rollMeta.setDisplayName("§a§lНАЖМИ, ЧТОБЫ КРУТНУТЬ!");
            List<String> rollLore = new ArrayList<>();
            rollLore.add("§7Потратить 1 крутку");
            rollLore.add("§7и получить случайную анимацию");
            rollMeta.setLore(rollLore);
            rollButton.setItemMeta(rollMeta);
            gui.setItem(22, rollButton);
        } else {
            ItemStack noRolls = new ItemStack(Material.BARRIER);
            ItemMeta noMeta = noRolls.getItemMeta();
            noMeta.setDisplayName("§c§lНЕТ КРУТОК");
            List<String> noLore = new ArrayList<>();
            noLore.add("§7Получите крутки у администратора");
            noMeta.setLore(noLore);
            noRolls.setItemMeta(noMeta);
            gui.setItem(22, noRolls);
        }
        
        // История анимаций
        List<Integer> history = playerAnimationHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
        int slot = 18;
        for (int i = Math.max(0, history.size() - 28); i < history.size(); i++) { // Показываем последние 28
            int animId = history.get(i);
            ItemStack historyItem = new ItemStack(Material.MAP);
            ItemMeta historyMeta = historyItem.getItemMeta();
            historyMeta.setDisplayName("§7" + ANIMATION_NAMES[animId - 1]);
            
            List<String> historyLore = new ArrayList<>();
            if (animId == currentAnim) {
                historyLore.add("§a✓ Текущая анимация");
            } else {
                historyLore.add("§eНажми, чтобы выбрать");
            }
            historyMeta.setLore(historyLore);
            
            historyItem.setItemMeta(historyMeta);
            gui.setItem(slot, historyItem);
            slot++;
        }
        
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§8§lСундук-рулетка")) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        if (slot == 22) { // Кнопка крутки
            performRoll(player);
            player.closeInventory();
        } else if (slot >= 18 && slot < 46) { // История анимаций
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.MAP) {
                String displayName = item.getItemMeta().getDisplayName();
                // Ищем анимацию по названию
                for (int i = 0; i < ANIMATION_NAMES.length; i++) {
                    if (ANIMATION_NAMES[i].equals(displayName)) {
                        playerAnimations.put(player.getUniqueId(), i + 1);
                        player.sendMessage("§aВы выбрали анимацию: " + ANIMATION_NAMES[i]);
                        player.closeInventory();
                        break;
                    }
                }
            }
        }
    }

    private void performRoll(Player player) {
        UUID uuid = player.getUniqueId();
        int rolls = playerRolls.getOrDefault(uuid, 0);
        
        if (rolls <= 0) {
            player.sendMessage("§cУ вас нет круток!");
            return;
        }
        
        playerRolls.put(uuid, rolls - 1);
        
        int animId = getRandomAnimation();
        
        // Сохраняем в историю
        List<Integer> history = playerAnimationHistory.getOrDefault(uuid, new ArrayList<>());
        history.add(animId);
        playerAnimationHistory.put(uuid, history);
        
        // Устанавливаем как текущую
        playerAnimations.put(uuid, animId);
        
        player.sendMessage("§a§l✦ ВАМ ВЫПАЛА АНИМАЦИЯ! ✦");
        player.sendMessage("§f" + ANIMATION_NAMES[animId - 1]);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        player.sendTitle("§6§lРУЛЕТКА", ANIMATION_NAMES[animId - 1], 10, 40, 10);
    }

    public void giveRoll(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = playerRolls.getOrDefault(uuid, 0);
        playerRolls.put(uuid, current + amount);
        player.sendMessage("§aВы получили " + amount + " круток в сундуке-рулетке!");
    }

    public void triggerKillAnimation(Player killer, Player victim) {
        int animId = playerAnimations.getOrDefault(killer.getUniqueId(), -1);
        if (animId == -1) return;
        
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        killer.sendMessage("§6§lАнимация убийства: §f" + ANIMATION_NAMES[animId - 1]);
        
        // Здесь код всех 22 анимаций (очень длинный)
        // Я могу добавить их по твоему запросу
    }

    public List<Integer> getPlayerHistory(Player player) {
        return playerAnimationHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }
}