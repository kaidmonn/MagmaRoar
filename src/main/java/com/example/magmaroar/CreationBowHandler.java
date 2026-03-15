package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CreationBowHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ForestInfo> activeForests = new HashMap<>();
    private final Set<Location> forestBlocks = new HashSet<>();
    private final Map<UUID, UUID> wolfOwners = new HashMap<>();
    
    private static final long COOLDOWN = 90 * 1000; // 90 секунд
    private static final int FOREST_SIZE = 20; // 20×20
    private static final int FOREST_DURATION = 25 * 20; // 25 секунд
    private static final int WOLF_COUNT = 5;
    private static final double WOLF_HEALTH_MULTIPLIER = 3.0;
    private static final int WOLF_STRENGTH = 4; // Сила 5 (amplifier 4)
    private static final int WOLF_SPEED = 2; // Скорость 3 (amplifier 2)

    private static class ForestInfo {
        Location center;
        List<Wolf> wolves;
        long endTime;
        UUID ownerId;

        ForestInfo(Location center, List<Wolf> wolves, long endTime, UUID ownerId) {
            this.center = center;
            this.wolves = wolves;
            this.endTime = endTime;
            this.ownerId = ownerId;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isCreationBow(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЛук сотворения перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location center = player.getTargetBlock(null, 200).getLocation().add(0.5, 0, 0.5);
            World world = player.getWorld();
            
            player.sendMessage("§2§lЛУК СОТВОРЕНИЯ! Создаётся лес 20×20...");
            world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            
            // 1. СОЗДАЁМ ПЛАТФОРМУ
            createPlatform(center, world);
            
            // 2. ВЫРАЩИВАЕМ ЛЕС
            growForest(center, world);
            
            // 3. СПАВНИМ ВОЛКОВ
            List<Wolf> wolves = spawnWolves(center, world, player);
            
            // Сохраняем информацию о лесе
            ForestInfo info = new ForestInfo(center, wolves, now + FOREST_DURATION * 50L, player.getUniqueId());
            activeForests.put(player.getUniqueId(), info);
            
            cooldowns.put(player.getUniqueId(), now);
            
            // Таймер на удаление леса
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeForest(player.getUniqueId());
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), FOREST_DURATION);
            
            event.setCancelled(true);
        }
    }

    private void createPlatform(Location center, World world) {
        int half = FOREST_SIZE / 2;
        
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                // Земля
                Location groundLoc = center.clone().add(x, -1, z);
                groundLoc.getBlock().setType(Material.DIRT);
                forestBlocks.add(groundLoc.clone());
                
                // Трава сверху
                Location grassLoc = center.clone().add(x, 0, z);
                grassLoc.getBlock().setType(Material.GRASS_BLOCK);
                forestBlocks.add(grassLoc.clone());
            }
        }
        
        // Барьеры по краям (непроходимые)
        for (int x = -half; x <= half; x++) {
            for (int y = 0; y <= 3; y++) {
                Location northBarrier = center.clone().add(x, y, -half - 1);
                northBarrier.getBlock().setType(Material.BARRIER);
                forestBlocks.add(northBarrier.clone());
                
                Location southBarrier = center.clone().add(x, y, half + 1);
                southBarrier.getBlock().setType(Material.BARRIER);
                forestBlocks.add(southBarrier.clone());
            }
        }
        
        for (int z = -half; z <= half; z++) {
            for (int y = 0; y <= 3; y++) {
                Location westBarrier = center.clone().add(-half - 1, y, z);
                westBarrier.getBlock().setType(Material.BARRIER);
                forestBlocks.add(westBarrier.clone());
                
                Location eastBarrier = center.clone().add(half + 1, y, z);
                eastBarrier.getBlock().setType(Material.BARRIER);
                forestBlocks.add(eastBarrier.clone());
            }
        }
    }

    private void growForest(Location center, World world) {
        int half = FOREST_SIZE / 2;
        Random random = new Random();
        
        for (int i = 0; i < 30; i++) { // 30 деревьев
            int x = random.nextInt(FOREST_SIZE) - half;
            int z = random.nextInt(FOREST_SIZE) - half;
            
            Location treeLoc = center.clone().add(x, 1, z);
            
            // Проверяем, что место свободно
            if (treeLoc.getBlock().getType() == Material.AIR) {
                // Создаём дерево (простой дуб)
                // Ствол
                for (int h = 1; h <= 4; h++) {
                    Location logLoc = center.clone().add(x, h, z);
                    logLoc.getBlock().setType(Material.OAK_LOG);
                    forestBlocks.add(logLoc.clone());
                }
                
                // Листва (шапка)
                for (int lx = -2; lx <= 2; lx++) {
                    for (int lz = -2; lz <= 2; lz++) {
                        for (int ly = 4; ly <= 6; ly++) {
                            if (Math.abs(lx) + Math.abs(lz) <= 3) {
                                Location leafLoc = center.clone().add(x + lx, ly, z + lz);
                                if (leafLoc.getBlock().getType() == Material.AIR) {
                                    leafLoc.getBlock().setType(Material.OAK_LEAVES);
                                    forestBlocks.add(leafLoc.clone());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private List<Wolf> spawnWolves(Location center, World world, Player owner) {
        List<Wolf> wolves = new ArrayList<>();
        int half = FOREST_SIZE / 2;
        Random random = new Random();
        
        for (int i = 0; i < WOLF_COUNT; i++) {
            int x = random.nextInt(FOREST_SIZE) - half;
            int z = random.nextInt(FOREST_SIZE) - half;
            
            Location wolfLoc = center.clone().add(x, 1, z);
            
            Wolf wolf = world.spawn(wolfLoc, Wolf.class);
            
            // Характеристики
            wolf.setTamed(true);
            wolf.setOwner(owner);
            wolf.setHealth(wolf.getMaxHealth() * WOLF_HEALTH_MULTIPLIER);
            wolf.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20 * WOLF_HEALTH_MULTIPLIER);
            
            // Сила 5 (атака)
            wolf.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, 
                FOREST_DURATION * 20, 
                WOLF_STRENGTH, 
                false, false
            ));
            
            // Скорость 3
            wolf.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 
                FOREST_DURATION * 20, 
                WOLF_SPEED, 
                false, false
            ));
            
            wolf.setRemoveWhenFarAway(false);
            wolf.setPersistent(true);
            
            wolves.add(wolf);
            wolfOwners.put(wolf.getUniqueId(), owner.getUniqueId());
        }
        
        return wolves;
    }

    private void removeForest(UUID ownerId) {
        ForestInfo info = activeForests.remove(ownerId);
        if (info == null) return;
        
        // Удаляем блоки леса
        for (Location loc : forestBlocks) {
            if (loc.getBlock().getType() != Material.AIR) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        forestBlocks.clear();
        
        // Удаляем волков
        for (Wolf wolf : info.wolves) {
            if (!wolf.isDead()) {
                wolf.remove();
            }
            wolfOwners.remove(wolf.getUniqueId());
        }
        
        Player owner = MagmaRoarPlugin.getInstance().getServer().getPlayer(ownerId);
        if (owner != null) {
            owner.sendMessage("§2Лес сотворения исчез.");
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        // Волки атакуют всех, кроме владельца
        if (event.getEntity() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getEntity();
            UUID ownerId = wolfOwners.get(wolf.getUniqueId());
            
            if (ownerId != null && event.getTarget() instanceof Player && 
                ((Player) event.getTarget()).getUniqueId().equals(ownerId)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Запрещаем ломать блоки в лесу
        if (forestBlocks.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cНельзя ломать блоки в лесу сотворения!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Запрещаем ставить блоки в лесу
        if (forestBlocks.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cНельзя ставить блоки в лесу сотворения!");
        }
    }

    private boolean isCreationBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лук сотворения");
    }
}