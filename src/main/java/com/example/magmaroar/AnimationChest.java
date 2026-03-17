package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private final Map<UUID, Location> playerChestLocation = new HashMap<>();
    private final Map<UUID, Boolean> openingAnimation = new HashMap<>();
    private final Random random = new Random();
    
    private static final String CHEST_NAME = "§6§lСундук-рулетка";
    
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
    
    private final Material[] ANIMATION_MATERIALS = {
        Material.TNT,                    // 1. Взрыв внутри
        Material.WITHER_SKELETON_SKULL, // 2. Визер-скелеты
        Material.ANVIL,                  // 3. Наковальня
        Material.SCULK,                   // 4. Варден-выстрел
        Material.CAKE,                    // 5. Курицы и тортик
        Material.FIREWORK_ROCKET,        // 6. Фейерверки
        Material.LIGHTNING_ROD,           // 7. Молния
        Material.WATER_BUCKET,            // 8. Дождь
        Material.MOSSY_COBBLESTONE,       // 9. Крест
        Material.SCULK_CATALYST,          // 10. Варден
        Material.MAGMA_BLOCK,             // 11. Метеорит
        Material.FEATHER,                  // 12. Невесомость
        Material.POPPY,                    // 13. Цветочная поляна
        Material.NETHER_STAR,              // 14. Звёздный дождь
        Material.GLASS,                    // 15. Невидимая стена
        Material.BONE,                     // 16. Танец скелетов
        Material.KELP,                     // 17. Водоворот
        Material.BLAZE_POWDER,             // 18. Огненный феникс
        Material.AMETHYST_CLUSTER,         // 19. Кристаллы
        Material.COAL_BLOCK,                // 20. Теневые копии
        Material.SAND,                      // 21. Песочные часы
        Material.WHITE_WOOL                 // 22. Радуга
    };
    
    private final int[] ANIMATION_CHANCES = {
        5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4
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

    private String getAnimationName(int id) {
        if (id < 1 || id > ANIMATION_NAMES.length) return "§7Неизвестно";
        return ANIMATION_NAMES[id - 1];
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
        playerChestLocation.put(player.getUniqueId(), chest.getLocation());
        openMainMenu(player);
    }

    private void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8§lСундук-рулетка");
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }
        
        // Кнопка 1: Крутить
        ItemStack rollButton = new ItemStack(Material.ENDER_CHEST);
        ItemMeta rollMeta = rollButton.getItemMeta();
        rollMeta.setDisplayName("§a§lКРУТНУТЬ");
        List<String> rollLore = new ArrayList<>();
        rollLore.add("§7Потратить 1 крутку");
        rollLore.add("§7и получить случайную анимацию");
        rollMeta.setLore(rollLore);
        rollButton.setItemMeta(rollMeta);
        gui.setItem(11, rollButton);
        
        // Кнопка 2: Выбрать анимацию
        ItemStack selectButton = new ItemStack(Material.BOOK);
        ItemMeta selectMeta = selectButton.getItemMeta();
        selectMeta.setDisplayName("§e§lВЫБРАТЬ АНИМАЦИЮ");
        List<String> selectLore = new ArrayList<>();
        selectLore.add("§7Выберите анимацию из");
        selectLore.add("§7ранее выбитых");
        selectMeta.setLore(selectLore);
        selectButton.setItemMeta(selectMeta);
        gui.setItem(13, selectButton);
        
        // Кнопка 3: Запросить крутку
        ItemStack requestButton = new ItemStack(Material.PAPER);
        ItemMeta requestMeta = requestButton.getItemMeta();
        requestMeta.setDisplayName("§d§lЗАПРОСИТЬ КРУТКУ");
        List<String> requestLore = new ArrayList<>();
        requestLore.add("§7Отправить запрос администратору");
        requestMeta.setLore(requestLore);
        requestButton.setItemMeta(requestMeta);
        gui.setItem(15, requestButton);
        
        // Информация о крутках
        int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
        ItemStack infoItem = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§lКруток: §f" + rolls);
        List<String> infoLore = new ArrayList<>();
        
        int currentAnim = playerAnimations.getOrDefault(player.getUniqueId(), -1);
        if (currentAnim != -1) {
            infoLore.add("§aТекущая анимация:");
            infoLore.add("§f" + getAnimationName(currentAnim));
        } else {
            infoLore.add("§cНет выбранной анимации");
        }
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem);
        
        player.openInventory(gui);
    }

    private void openSelectionMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§lВыберите анимацию");
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glass);
            gui.setItem(i + 45, glass);
        }
        
        List<Integer> history = playerAnimationHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
        Set<Integer> uniqueAnimations = new LinkedHashSet<>(history);
        
        int slot = 9;
        for (int animId : uniqueAnimations) {
            if (slot >= 45) break;
            
            ItemStack animItem = new ItemStack(ANIMATION_MATERIALS[animId - 1]);
            ItemMeta animMeta = animItem.getItemMeta();
            animMeta.setDisplayName(ANIMATION_NAMES[animId - 1]);
            
            List<String> animLore = new ArrayList<>();
            animLore.add("§7Нажмите, чтобы выбрать");
            animMeta.setLore(animLore);
            
            animItem.setItemMeta(animMeta);
            gui.setItem(slot, animItem);
            slot++;
        }
        
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals("§8§lСундук-рулетка")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 11) {
                int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
                if (rolls <= 0) {
                    player.sendMessage("§cУ вас нет круток!");
                    player.closeInventory();
                    return;
                }
                
                Location chestLoc = playerChestLocation.get(player.getUniqueId());
                if (chestLoc == null) {
                    player.sendMessage("§cОшибка: не удалось найти сундук! Попробуйте открыть его заново.");
                    player.closeInventory();
                    return;
                }
                
                player.closeInventory();
                startSpinAnimation(player, chestLoc);
            }
            else if (slot == 13) {
                openSelectionMenu(player);
            }
            else if (slot == 15) {
                player.closeInventory();
                requestRoll(player);
            }
        }
        else if (title.equals("§8§lВыберите анимацию")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot >= 9 && slot < 45) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                    String displayName = item.getItemMeta().getDisplayName();
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
    }

    private void startSpinAnimation(Player player, Location chestLoc) {
        UUID uuid = player.getUniqueId();
        openingAnimation.put(uuid, true);
        
        World world = chestLoc.getWorld();
        if (world == null) return;
        
        player.sendMessage("§6§lКРУТКА... Подождите 5 секунд!");
        
        // Удаляем все старые блоки перед началом
        for (Entity e : world.getEntities()) {
            if (e instanceof FallingBlock && e.getCustomName() != null) {
                e.remove();
            }
        }
        
        List<FallingBlock> rotatingBlocks = new ArrayList<>();
        
        // Создаем 22 блока
        for (int i = 0; i < 22; i++) {
            try {
                double angle = i * (2 * Math.PI / 22);
                Location spawnLoc = chestLoc.clone().add(Math.cos(angle) * 4, 2, Math.sin(angle) * 4);
                FallingBlock block = world.spawnFallingBlock(spawnLoc, ANIMATION_MATERIALS[i].createBlockData());
                block.setDropItem(false);
                block.setHurtEntities(false);
                block.setGravity(false);
                block.setVelocity(new Vector(0, 0, 0));
                block.setCustomName("§f" + ANIMATION_NAMES[i]);
                block.setCustomNameVisible(true);
                block.setPersistent(false);
                rotatingBlocks.add(block);
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось создать блок для анимации " + i);
            }
        }
        
        // Форсированное удаление через 6 секунд
        BukkitRunnable forceRemove = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : world.getEntities()) {
                    if (e instanceof FallingBlock && e.getCustomName() != null) {
                        e.remove();
                    }
                }
                openingAnimation.put(uuid, false);
            }
        };
        forceRemove.runTaskLater(plugin, 120L);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) {
                    // Выбираем случайную анимацию
                    int animId = getRandomAnimation();
                    
                    // Удаляем старые блоки
                    for (FallingBlock block : rotatingBlocks) {
                        if (block != null && !block.isDead()) {
                            block.remove();
                        }
                    }
                    rotatingBlocks.clear();
                    
                    // Создаем 8 победных блоков
                    List<FallingBlock> winnerBlocks = new ArrayList<>();
                    for (int i = 0; i < 8; i++) {
                        try {
                            double angle = i * (2 * Math.PI / 8);
                            Location spawnLoc = chestLoc.clone().add(Math.cos(angle) * 2.5, 2, Math.sin(angle) * 2.5);
                            FallingBlock block = world.spawnFallingBlock(spawnLoc, ANIMATION_MATERIALS[animId - 1].createBlockData());
                            block.setDropItem(false);
                            block.setHurtEntities(false);
                            block.setGravity(false);
                            block.setVelocity(new Vector(0, 0, 0));
                            block.setCustomName("§6§l" + ANIMATION_NAMES[animId - 1]);
                            block.setCustomNameVisible(true);
                            block.setPersistent(false);
                            winnerBlocks.add(block);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Не удалось создать победный блок");
                        }
                    }
                    
                    // Выдаем анимацию
                    performRoll(player, animId);
                    
                    // Удаляем победные блоки через 1.5 секунды
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (FallingBlock block : winnerBlocks) {
                                if (block != null && !block.isDead()) {
                                    block.remove();
                                }
                            }
                            
                            // Финальная очистка
                            for (Entity e : world.getEntities()) {
                                if (e instanceof FallingBlock && e.getCustomName() != null) {
                                    e.remove();
                                }
                            }
                            
                            openingAnimation.put(uuid, false);
                            forceRemove.cancel();
                        }
                    }.runTaskLater(plugin, 30L);
                    
                    this.cancel();
                    return;
                }
                
                // Вращаем блоки
                for (int i = 0; i < rotatingBlocks.size(); i++) {
                    FallingBlock block = rotatingBlocks.get(i);
                    if (block == null || block.isDead()) continue;
                    
                    double baseAngle = i * (2 * Math.PI / rotatingBlocks.size());
                    double angle = baseAngle + (ticks * 0.03);
                    double radius = 4 + Math.sin(ticks * 0.05) * 0.5;
                    double yOffset = Math.sin(ticks * 0.1 + i) * 1.5;
                    
                    try {
                        Location newLoc = chestLoc.clone().add(
                            Math.cos(angle) * radius,
                            2 + yOffset,
                            Math.sin(angle) * radius
                        );
                        block.teleport(newLoc);
                    } catch (Exception e) {
                        // Игнорируем ошибки телепорта
                    }
                }
                
                // Частицы
                world.spawnParticle(Particle.PORTAL, chestLoc.clone().add(0, 2, 0), 5, 2, 1, 2, 0);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void requestRoll(Player player) {
        Player admin = Bukkit.getPlayer("kaidmonngrief");
        if (admin == null || !admin.isOnline()) {
            player.sendMessage("§cАдминистратор не в сети!");
            return;
        }
        
        admin.sendMessage("§d§l═══════════════════════");
        admin.sendMessage("§d§lЗАПРОС НА КРУТКУ");
        admin.sendMessage("§fИгрок: §a" + player.getName());
        admin.sendMessage("§fКруток сейчас: §e" + playerRolls.getOrDefault(player.getUniqueId(), 0));
        admin.sendMessage("§d§l═══════════════════════");
        admin.sendMessage("§aНажмите, чтобы выдать 1 крутку:");
        admin.sendMessage("§e/giveroll " + player.getName() + " 1");
        
        player.sendMessage("§aЗапрос отправлен администратору!");
    }

    private void performRoll(Player player, int animId) {
        UUID uuid = player.getUniqueId();
        int rolls = playerRolls.getOrDefault(uuid, 0);
        
        if (rolls <= 0) return;
        
        playerRolls.put(uuid, rolls - 1);
        
        List<Integer> history = playerAnimationHistory.getOrDefault(uuid, new ArrayList<>());
        history.add(animId);
        playerAnimationHistory.put(uuid, history);
        
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
        
        // Здесь код анимаций убийства
    }
}