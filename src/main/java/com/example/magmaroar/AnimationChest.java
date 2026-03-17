package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AnimationChest implements Listener {

    private final MagmaRoarPlugin plugin;
    private final Map<UUID, Integer> playerRolls = new HashMap<>();
    private final Map<UUID, Integer> playerAnimations = new HashMap<>();
    private final Map<UUID, List<Integer>> playerAnimationHistory = new HashMap<>();
    private final Map<UUID, Location> playerChestLocation = new HashMap<>();
    private final Set<UUID> spinningPlayers = new HashSet<>();
    private final Set<UUID> rollingPlayers = new HashSet<>();
    private final Random random = new Random();
    
    private File dataFile;
    private FileConfiguration dataConfig;
    
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
        setupDataFile();
        loadData();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "animation_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл animation_data.yml");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        // Сохраняем крутки
        for (Map.Entry<UUID, Integer> entry : playerRolls.entrySet()) {
            dataConfig.set("rolls." + entry.getKey().toString(), entry.getValue());
        }
        
        // Сохраняем текущие анимации
        for (Map.Entry<UUID, Integer> entry : playerAnimations.entrySet()) {
            dataConfig.set("current." + entry.getKey().toString(), entry.getValue());
        }
        
        // Сохраняем историю анимаций
        for (Map.Entry<UUID, List<Integer>> entry : playerAnimationHistory.entrySet()) {
            dataConfig.set("history." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить animation_data.yml");
        }
    }

    private void loadData() {
        // Загружаем крутки
        if (dataConfig.contains("rolls")) {
            for (String key : dataConfig.getConfigurationSection("rolls").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int rolls = dataConfig.getInt("rolls." + key);
                    playerRolls.put(uuid, rolls);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверный UUID в данных: " + key);
                }
            }
        }
        
        // Загружаем текущие анимации
        if (dataConfig.contains("current")) {
            for (String key : dataConfig.getConfigurationSection("current").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int anim = dataConfig.getInt("current." + key);
                    playerAnimations.put(uuid, anim);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверный UUID в данных: " + key);
                }
            }
        }
        
        // Загружаем историю анимаций
        if (dataConfig.contains("history")) {
            for (String key : dataConfig.getConfigurationSection("history").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<Integer> history = dataConfig.getIntegerList("history." + key);
                    playerAnimationHistory.put(uuid, history);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверный UUID в данных: " + key);
                }
            }
        }
        
        plugin.getLogger().info("Загружены данные анимаций для " + playerRolls.size() + " игроков");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Восстанавливаем данные при входе
        if (playerRolls.containsKey(uuid)) {
            int rolls = playerRolls.get(uuid);
            player.sendMessage("§aУ вас " + rolls + " круток в сундуке-рулетке!");
        }
        
        if (playerAnimations.containsKey(uuid)) {
            int anim = playerAnimations.get(uuid);
            player.sendMessage("§aВаша текущая анимация: " + ANIMATION_NAMES[anim - 1]);
        }
    }

    public int getPlayerAnimation(Player player) {
        return playerAnimations.getOrDefault(player.getUniqueId(), -1);
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
        
        if (!spinningPlayers.isEmpty()) {
            player.sendMessage("§cСейчас кто-то уже крутит рулетку! Подождите.");
            return;
        }
        
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
        
        int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
        
        ItemStack infoItem = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§lКруток: §f" + rolls);
        List<String> lore = new ArrayList<>();
        lore.add("§7Нажми на сундук, чтобы");
        lore.add("§7потратить 1 крутку");
        lore.add("§7Шанс каждой анимации: §aразный");
        lore.add("");
        
        int currentAnim = playerAnimations.getOrDefault(player.getUniqueId(), -1);
        if (currentAnim != -1) {
            lore.add("§aТекущая анимация:");
            lore.add("§f" + getAnimationName(currentAnim));
        } else {
            lore.add("§cУ вас нет выбранной анимации");
        }
        
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem);
        
        ItemStack rollButton = new ItemStack(Material.ENDER_CHEST);
        ItemMeta rollMeta = rollButton.getItemMeta();
        rollMeta.setDisplayName("§a§lКРУТНУТЬ");
        List<String> rollLore = new ArrayList<>();
        rollLore.add("§7Потратить 1 крутку");
        rollLore.add("§7и получить случайную анимацию");
        rollMeta.setLore(rollLore);
        rollButton.setItemMeta(rollMeta);
        gui.setItem(11, rollButton);
        
        ItemStack selectButton = new ItemStack(Material.BOOK);
        ItemMeta selectMeta = selectButton.getItemMeta();
        selectMeta.setDisplayName("§e§lВЫБРАТЬ АНИМАЦИЮ");
        List<String> selectLore = new ArrayList<>();
        selectLore.add("§7Выберите анимацию из");
        selectLore.add("§7ранее выбитых");
        selectMeta.setLore(selectLore);
        selectButton.setItemMeta(selectMeta);
        gui.setItem(13, selectButton);
        
        ItemStack requestButton = new ItemStack(Material.PAPER);
        ItemMeta requestMeta = requestButton.getItemMeta();
        requestMeta.setDisplayName("§d§lЗАПРОСИТЬ КРУТКУ");
        List<String> requestLore = new ArrayList<>();
        requestLore.add("§7Отправить запрос администратору");
        requestMeta.setLore(requestLore);
        requestButton.setItemMeta(requestMeta);
        gui.setItem(15, requestButton);
        
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
                if (rollingPlayers.contains(player.getUniqueId())) {
                    player.sendMessage("§cКрутка уже запущена!");
                    player.closeInventory();
                    return;
                }
                
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
                            saveData(); // Сохраняем после выбора
                            break;
                        }
                    }
                }
            }
        }
    }

    private void startSpinAnimation(Player player, Location chestLoc) {
        UUID uuid = player.getUniqueId();
        
        if (rollingPlayers.contains(uuid)) {
            player.sendMessage("§cКрутка уже запущена!");
            return;
        }
        
        int rolls = playerRolls.getOrDefault(uuid, 0);
        if (rolls <= 0) {
            player.sendMessage("§cУ вас нет круток!");
            return;
        }
        
        playerRolls.put(uuid, rolls - 1);
        rollingPlayers.add(uuid);
        spinningPlayers.add(uuid);
        
        World world = chestLoc.getWorld();
        if (world == null) return;
        
        player.sendMessage("§6§lКРУТКА... Подождите 5 секунд!");
        
        for (Entity e : world.getEntities()) {
            if (e instanceof FallingBlock && e.getCustomName() != null) {
                e.remove();
            }
        }
        
        List<FallingBlock> rotatingBlocks = new ArrayList<>();
        
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
        
        BukkitRunnable forceRemove = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : world.getEntities()) {
                    if (e instanceof FallingBlock && e.getCustomName() != null) {
                        e.remove();
                    }
                }
                spinningPlayers.remove(uuid);
                rollingPlayers.remove(uuid);
            }
        };
        forceRemove.runTaskLater(plugin, 120L);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) {
                    int animId = getRandomAnimation();
                    
                    for (FallingBlock block : rotatingBlocks) {
                        if (block != null && !block.isDead()) {
                            block.remove();
                        }
                    }
                    rotatingBlocks.clear();
                    
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
                    
                    performRoll(player, animId);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (FallingBlock block : winnerBlocks) {
                                if (block != null && !block.isDead()) {
                                    block.remove();
                                }
                            }
                            
                            for (Entity e : world.getEntities()) {
                                if (e instanceof FallingBlock && e.getCustomName() != null) {
                                    e.remove();
                                }
                            }
                            
                            spinningPlayers.remove(uuid);
                            rollingPlayers.remove(uuid);
                            forceRemove.cancel();
                        }
                    }.runTaskLater(plugin, 30L);
                    
                    this.cancel();
                    return;
                }
                
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
                    } catch (Exception e) {}
                }
                
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
        
        List<Integer> history = playerAnimationHistory.getOrDefault(uuid, new ArrayList<>());
        history.add(animId);
        playerAnimationHistory.put(uuid, history);
        
        playerAnimations.put(uuid, animId);
        
        player.sendMessage("§a§l✦ ВАМ ВЫПАЛА АНИМАЦИЯ! ✦");
        player.sendMessage("§f" + ANIMATION_NAMES[animId - 1]);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendTitle("§6§lРУЛЕТКА", ANIMATION_NAMES[animId - 1], 10, 40, 10);
        
        saveData(); // Сохраняем после выдачи
    }

    public void giveRoll(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = playerRolls.getOrDefault(uuid, 0);
        playerRolls.put(uuid, current + amount);
        player.sendMessage("§aВы получили " + amount + " круток в сундуке-рулетке!");
        saveData(); // Сохраняем после выдачи круток
    }

    public void triggerKillAnimation(Player killer, Player victim, int animId) {
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        killer.sendMessage("§6§lАнимация убийства: §f" + ANIMATION_NAMES[animId - 1]);
        
        switch (animId) {
            case 1:
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                world.spawnParticle(Particle.EXPLOSION, loc, 20, 1, 1, 1, 0);
                break;
                
            case 2:
                new BukkitRunnable() {
                    int ticks = 0;
                    List<WitherSkeleton> skeletons = new ArrayList<>();
                    
                    @Override
                    public void run() {
                        if (ticks == 0) {
                            for (int i = 0; i < 4; i++) {
                                Location spawnLoc = loc.clone().add(
                                    Math.cos(i * Math.PI/2) * 3,
                                    0,
                                    Math.sin(i * Math.PI/2) * 3
                                );
                                WitherSkeleton skelly = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                                skelly.setAI(false);
                                skelly.setInvulnerable(true);
                                skelly.setSilent(true);
                                skeletons.add(skelly);
                            }
                        }
                        
                        if (ticks >= 100) {
                            for (WitherSkeleton skelly : skeletons) {
                                skelly.remove();
                            }
                            this.cancel();
                            return;
                        }
                        
                        for (int i = 0; i < skeletons.size(); i++) {
                            WitherSkeleton skelly = skeletons.get(i);
                            double angle = (ticks * 0.05) + (i * Math.PI/2);
                            Location newLoc = loc.clone().add(
                                Math.cos(angle) * 3,
                                0,
                                Math.sin(angle) * 3
                            );
                            skelly.teleport(newLoc);
                        }
                        
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 3:
                Location anvilLoc = loc.clone().add(0, 10, 0);
                FallingBlock anvil = world.spawnFallingBlock(anvilLoc, Material.ANVIL.createBlockData());
                anvil.setDropItem(false);
                anvil.setHurtEntities(false);
                world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        anvil.remove();
                    }
                }.runTaskLater(plugin, 40L);
                break;
                
            case 4:
                world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
                world.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                break;
                
            case 5:
                for (int i = 0; i < 5; i++) {
                    Chicken chicken = (Chicken) world.spawnEntity(loc.clone().add(random.nextInt(3)-1, 0, random.nextInt(3)-1), EntityType.CHICKEN);
                    chicken.setInvulnerable(true);
                    chicken.setAI(false);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            chicken.remove();
                        }
                    }.runTaskLater(plugin, 100L);
                }
                
                Location cakeLoc = loc.clone().add(0, 1, 0);
                cakeLoc.getBlock().setType(Material.CAKE);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (cakeLoc.getBlock().getType() == Material.CAKE) {
                            cakeLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }.runTaskLater(plugin, 100L);
                break;
                
            case 6:
                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if (count >= 5) {
                            this.cancel();
                            return;
                        }
                        
                        Firework firework = world.spawn(loc.clone().add(random.nextInt(3)-1, 0, random.nextInt(3)-1), Firework.class);
                        FireworkMeta meta = firework.getFireworkMeta();
                        meta.addEffect(FireworkEffect.builder()
                            .withColor(Color.RED, Color.BLUE, Color.GREEN)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .build());
                        meta.setPower(1);
                        firework.setFireworkMeta(meta);
                        
                        count++;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                break;
                
            case 7:
                world.strikeLightningEffect(loc);
                break;
                
            case 8:
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) {
                            this.cancel();
                            return;
                        }
                        world.spawnParticle(Particle.RAIN, loc.clone().add(random.nextInt(5)-2, 2, random.nextInt(5)-2), 5, 0.5, 0.5, 0.5, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 9:
                for (int y = 1; y <= 3; y++) {
                    Location blockLoc = loc.clone().add(0, y, 0);
                    blockLoc.getBlock().setType(Material.MOSSY_COBBLESTONE);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (blockLoc.getBlock().getType() == Material.MOSSY_COBBLESTONE) {
                                blockLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }.runTaskLater(plugin, 100L);
                }
                for (int x = -1; x <= 1; x += 2) {
                    Location blockLoc = loc.clone().add(x, 2, 0);
                    blockLoc.getBlock().setType(Material.MOSSY_COBBLESTONE);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (blockLoc.getBlock().getType() == Material.MOSSY_COBBLESTONE) {
                                blockLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }.runTaskLater(plugin, 100L);
                }
                break;
                
            case 10:
                Location wardenLoc = loc.clone().add(0, -2, 0);
                Warden warden = (Warden) world.spawnEntity(wardenLoc, EntityType.WARDEN);
                warden.setAI(false);
                warden.setInvulnerable(true);
                warden.setSilent(true);
                
                new BukkitRunnable() {
                    int y = -2;
                    @Override
                    public void run() {
                        if (y >= 0) {
                            new BukkitRunnable() {
                                int downY = 0;
                                @Override
                                public void run() {
                                    if (downY <= -2) {
                                        warden.remove();
                                        this.cancel();
                                        return;
                                    }
                                    warden.teleport(loc.clone().add(0, downY, 0));
                                    downY--;
                                }
                            }.runTaskTimer(plugin, 20L, 2L);
                            this.cancel();
                            return;
                        }
                        warden.teleport(loc.clone().add(0, y, 0));
                        y++;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                break;
                
            case 11:
                Location meteorLoc = loc.clone().add(0, 20, 0);
                Fireball meteor = world.spawn(meteorLoc, Fireball.class);
                meteor.setVelocity(new Vector(0, -0.5, 0));
                meteor.setYield(0);
                meteor.setIsIncendiary(false);
                
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40 || meteor.isDead()) {
                            meteor.remove();
                            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                            world.spawnParticle(Particle.EXPLOSION, loc, 20, 2, 2, 2, 0);
                            this.cancel();
                            return;
                        }
                        world.spawnParticle(Particle.FLAME, meteor.getLocation(), 5, 1, 1, 1, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 12:
                victim.setVelocity(new Vector(0, 1.5, 0));
                world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) {
                            this.cancel();
                            return;
                        }
                        world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 13:
                Material[] flowers = {Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET};
                for (int i = 0; i < 8; i++) {
                    Location flowerLoc = loc.clone().add(random.nextInt(5)-2, 0, random.nextInt(5)-2);
                    flowerLoc.getBlock().setType(flowers[random.nextInt(flowers.length)]);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            flowerLoc.getBlock().setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, 100L);
                }
                
                for (int i = 0; i < 3; i++) {
                    Bee bee = (Bee) world.spawnEntity(loc.clone().add(random.nextInt(3)-1, 1, random.nextInt(3)-1), EntityType.BEE);
                    bee.setAI(false);
                    bee.setInvulnerable(true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            bee.remove();
                        }
                    }.runTaskLater(plugin, 100L);
                }
                break;
                
            case 14:
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) {
                            this.cancel();
                            return;
                        }
                        Location starLoc = loc.clone().add(random.nextInt(10)-5, 10, random.nextInt(10)-5);
                        world.spawnParticle(Particle.END_ROD, starLoc, 3, 0.2, 0.2, 0.2, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                break;
                
            case 15:
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                            Location wallLoc = loc.clone().add(x, 0, z);
                            world.spawnParticle(Particle.BLOCK_MARKER, wallLoc.clone().add(0.5, 1, 0.5), 1, 0, 0, 0, 0, Material.BARRIER.createBlockData());
                        }
                    }
                }
                break;
                
            case 16:
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI/2;
                    Location spawnLoc = loc.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
                    Skeleton skelly = (Skeleton) world.spawnEntity(spawnLoc, EntityType.SKELETON);
                    skelly.setAI(false);
                    skelly.setInvulnerable(true);
                    skelly.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                    skelly.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
                    skelly.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
                    skelly.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
                    
                    new BukkitRunnable() {
                        int tick = 0;
                        @Override
                        public void run() {
                            if (tick >= 100) {
                                skelly.remove();
                                this.cancel();
                                return;
                            }
                            float yaw = skelly.getLocation().getYaw() + 10;
                            skelly.getLocation().setYaw(yaw);
                            skelly.teleport(skelly.getLocation());
                            tick++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
                break;
                
            case 17:
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 80) {
                            this.cancel();
                            return;
                        }
                        double radius = 3 * (1 - ticks / 80.0);
                        for (int i = 0; i < 8; i++) {
                            double angle = (ticks * 0.2) + (i * Math.PI/4);
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            world.spawnParticle(Particle.CURRENT_DOWN, loc.clone().add(x, 1, z), 1, 0, 0, 0, 0);
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 18:
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) {
                            this.cancel();
                            return;
                        }
                        double y = ticks * 0.5;
                        for (int i = 0; i < 5; i++) {
                            double angle = (ticks * 0.5) + (i * Math.PI/2.5);
                            double radius = 2;
                            world.spawnParticle(Particle.FLAME, 
                                loc.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                                2, 0, 0, 0, 0);
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 19:
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI/3;
                    Location crystalLoc = loc.clone().add(Math.cos(angle) * 2, 1, Math.sin(angle) * 2);
                    crystalLoc.getBlock().setType(Material.AMETHYST_BLOCK);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (crystalLoc.getBlock().getType() == Material.AMETHYST_BLOCK) {
                                crystalLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }.runTaskLater(plugin, 80L);
                }
                break;
                
            case 20:
                for (int i = 0; i < 3; i++) {
                    Location copyLoc = loc.clone().add(random.nextInt(3)-1, 0, random.nextInt(3)-1);
                    ArmorStand copy = (ArmorStand) world.spawnEntity(copyLoc, EntityType.ARMOR_STAND);
                    copy.setVisible(true);
                    copy.setSmall(false);
                    copy.setArms(true);
                    copy.setItemInHand(new ItemStack(Material.IRON_SWORD));
                    copy.setInvulnerable(true);
                    copy.setGravity(false);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            copy.remove();
                        }
                    }.runTaskLater(plugin, 40L);
                }
                break;
                
            case 21:
                Location hourglassTop = loc.clone().add(0, 3, 0);
                Location hourglassBottom = loc.clone();
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) {
                            this.cancel();
                            return;
                        }
                        world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, hourglassTop, 2, 0.5, 0, 0.5, 0);
                        world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, hourglassBottom, 1, 0.5, 0, 0.5, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 22:
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) {
                            this.cancel();
                            return;
                        }
                        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.PURPLE};
                        for (int i = 0; i < 7; i++) {
                            double x = (i - 3) * 1.5;
                            double y = 2 + Math.sin(ticks * 0.1 + i) * 0.5;
                            world.spawnParticle(Particle.DUST, loc.clone().add(x, y, 0), 1, 0, 0, 0, 0, new Particle.DustOptions(colors[i % colors.length], 2));
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
        }
    }
}