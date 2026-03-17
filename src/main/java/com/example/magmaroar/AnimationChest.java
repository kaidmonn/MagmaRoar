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
        
        if (openingAnimation.getOrDefault(player.getUniqueId(), false)) {
            player.sendMessage("§cСундук уже открывается...");
            return;
        }
        
        // Проверяем наличие круток
        int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
        if (rolls <= 0) {
            player.sendMessage("§cУ вас нет круток! Получите их у администратора.");
            return;
        }
        
        startOpeningAnimation(player, chest.getLocation());
    }

    private void startOpeningAnimation(Player player, Location chestLoc) {
        UUID uuid = player.getUniqueId();
        openingAnimation.put(uuid, true);
        
        World world = chestLoc.getWorld();
        if (world == null) return;
        
        player.sendMessage("§6§lСундук открывается... Подождите 5 секунд!");
        
        // Выбираем случайную анимацию ЗАРАНЕЕ
        int animId = getRandomAnimation();
        
        // Определяем блоки для анимации в зависимости от анимации
        Material blockMaterial = getAnimationMaterial(animId);
        
        List<FallingBlock> rotatingBlocks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Location spawnLoc = chestLoc.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
            FallingBlock block = world.spawnFallingBlock(spawnLoc, blockMaterial.createBlockData());
            block.setDropItem(false);
            block.setHurtEntities(false);
            block.setGravity(false);
            block.setVelocity(new Vector(0, 0, 0));
            rotatingBlocks.add(block);
        }
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) {
                    // Удаляем блоки
                    for (FallingBlock block : rotatingBlocks) {
                        block.remove();
                    }
                    
                    // ВЫДАЕМ АНИМАЦИЮ
                    performRoll(player, animId);
                    
                    openingAnimation.put(uuid, false);
                    this.cancel();
                    return;
                }
                
                // Вращаем блоки
                for (int i = 0; i < rotatingBlocks.size(); i++) {
                    FallingBlock block = rotatingBlocks.get(i);
                    double angle = (ticks * 0.1) + (i * Math.PI / 4);
                    Location newLoc = chestLoc.clone().add(
                        Math.cos(angle) * 3, 
                        1 + Math.sin(ticks * 0.1) * 0.5, 
                        Math.sin(angle) * 3
                    );
                    block.teleport(newLoc);
                }
                
                // Частицы
                world.spawnParticle(Particle.PORTAL, chestLoc.clone().add(0, 1, 0), 5, 1, 1, 1, 0);
                world.spawnParticle(getAnimationParticle(animId), chestLoc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Material getAnimationMaterial(int animId) {
        switch (animId) {
            case 1: return Material.TNT; // Взрыв внутри
            case 2: return Material.WITHER_SKELETON_SKULL; // Визер-скелеты
            case 3: return Material.ANVIL; // Наковальня
            case 4: return Material.SCULK; // Варден-выстрел
            case 5: return Material.CAKE; // Курицы и тортик
            case 6: return Material.FIREWORK_ROCKET; // Фейерверки
            case 7: return Material.LIGHTNING_ROD; // Молния
            case 8: return Material.WATER_BUCKET; // Дождь
            case 9: return Material.MOSSY_COBBLESTONE; // Крест
            case 10: return Material.SCULK_CATALYST; // Варден
            case 11: return Material.MAGMA_BLOCK; // Метеорит
            case 12: return Material.FEATHER; // Невесомость
            case 13: return Material.POPPY; // Цветочная поляна
            case 14: return Material.NETHER_STAR; // Звёздный дождь
            case 15: return Material.GLASS; // Невидимая стена
            case 16: return Material.BONE; // Танец скелетов
            case 17: return Material.KELP; // Водоворот
            case 18: return Material.BLAZE_POWDER; // Огненный феникс
            case 19: return Material.AMETHYST_CLUSTER; // Кристаллы
            case 20: return Material.COAL_BLOCK; // Теневые копии
            case 21: return Material.SAND; // Песочные часы
            case 22: return Material.WHITE_WOOL; // Радуга
            default: return Material.DIRT;
        }
    }

    private Particle getAnimationParticle(int animId) {
        switch (animId) {
            case 1: return Particle.EXPLOSION;
            case 2: return Particle.SOUL;
            case 3: return Particle.ITEM;
            case 4: return Particle.SONIC_BOOM;
            case 5: return Particle.HEART;
            case 6: return Particle.FIREWORK;
            case 7: return Particle.ELECTRIC_SPARK;
            case 8: return Particle.RAIN;
            case 9: return Particle.ASH;
            case 10: return Particle.SCULK_SOUL;
            case 11: return Particle.FLAME;
            case 12: return Particle.CLOUD;
            case 13: return Particle.HAPPY_VILLAGER;
            case 14: return Particle.END_ROD;
            case 15: return Particle.WHITE_ASH;
            case 16: return Particle.SOUL_FIRE_FLAME;
            case 17: return Particle.CURRENT_DOWN;
            case 18: return Particle.LAVA;
            case 19: return Particle.GLOW;
            case 20: return Particle.SMOKE;
            case 21: return Particle.FALLING_OBSIDIAN_TEAR;
            case 22: return Particle.WAX_ON;
            default: return Particle.PORTAL;
        }
    }

    private void performRoll(Player player, int animId) {
        UUID uuid = player.getUniqueId();
        int rolls = playerRolls.getOrDefault(uuid, 0);
        
        if (rolls <= 0) return;
        
        // Тратим крутку
        playerRolls.put(uuid, rolls - 1);
        
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
        
        // Здесь код анимаций убийства (можно добавить позже)
    }
}