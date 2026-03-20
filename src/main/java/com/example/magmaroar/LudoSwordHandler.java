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

    private final Map<UUID, LudoStats> stats = new HashMap<>();
    private final Random random = new Random();
    
    private static final int ITEM_DURATION = 30;
    private static final int COOLDOWN_DURATION = 35;
    
    private enum LudoMode {
        FROST, SHADOW, SPIDER, MJOLNIR, DEATH_SCYTHE,
        STORM, REAPER, DRAGON, EXCALIBUR, LIGHT_MACE, JACKPOT
    }
    
    private static class LudoStats {
        LudoMode currentMode = null;
        long modeEndTime = 0;
        long cooldownEndTime = 0;
        boolean isRolling = false;
        ItemStack originalItem = null;
        int slot = -1;
    }
    
    private static final Map<LudoMode, String> MODE_NAMES = new HashMap<>();
    
    static {
        MODE_NAMES.put(LudoMode.FROST, "§bМорозный меч");
        MODE_NAMES.put(LudoMode.SHADOW, "§8Теневой меч");
        MODE_NAMES.put(LudoMode.SPIDER, "§2Паучий клинок");
        MODE_NAMES.put(LudoMode.MJOLNIR, "§eМьёльнир");
        MODE_NAMES.put(LudoMode.DEATH_SCYTHE, "§cКоса смерти");
        MODE_NAMES.put(LudoMode.STORM, "§9Клинок бури");
        MODE_NAMES.put(LudoMode.REAPER, "§5Коса жнеца");
        MODE_NAMES.put(LudoMode.DRAGON, "§dКатана дракона");
        MODE_NAMES.put(LudoMode.EXCALIBUR, "§6Экскалибур");
        MODE_NAMES.put(LudoMode.LIGHT_MACE, "§fЛегкая булава");
        MODE_NAMES.put(LudoMode.JACKPOT, "§d§lДЖЕКПОТ");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLudoSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            LudoStats playerStats = stats.computeIfAbsent(player.getUniqueId(), k -> new LudoStats());
            
            if (playerStats.cooldownEndTime > now) {
                long secondsLeft = (playerStats.cooldownEndTime - now) / 1000;
                player.sendMessage("§cЛудо-меч перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            if (playerStats.currentMode != null) {
                player.sendMessage("§cУ вас уже есть активный предмет!");
                event.setCancelled(true);
                return;
            }
            
            if (playerStats.isRolling) {
                player.sendMessage("§cРулетка уже крутится!");
                event.setCancelled(true);
                return;
            }
            
            playerStats.slot = player.getInventory().getHeldItemSlot();
            playerStats.originalItem = item.clone();
            playerStats.isRolling = true;
            
            startRoulette(player);
            event.setCancelled(true);
        }
    }

    private void startRoulette(Player player) {
        player.sendMessage("§6§l🔄 ЛУДО-МЕЧ: КРУТИТСЯ РУЛЕТКА...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    
                    LudoStats playerStats = stats.get(player.getUniqueId());
                    if (playerStats != null) {
                        playerStats.isRolling = false;
                    }
                    
                    LudoMode selected = selectRandomMode();
                    player.sendMessage("§6§l═══════════════════════");
                    player.sendMessage("§6§l  ВЫПАЛО: " + MODE_NAMES.get(selected));
                    player.sendMessage("§6§l═══════════════════════");
                    
                    playModeSound(player, selected);
                    giveOriginalItem(player, selected);
                    
                    this.cancel();
                    return;
                }
                
                if (ticks % 4 == 0) {
                    LudoMode randomMode = getRandomMode();
                    player.sendMessage("§8> " + MODE_NAMES.get(randomMode));
                }
                
                if (ticks % 8 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private LudoMode selectRandomMode() {
        double r = random.nextDouble() * 100;
        
        if (r < 5) return LudoMode.JACKPOT;
        
        int index = (int) ((r - 5) / 9.5);
        LudoMode[] modes = {
            LudoMode.FROST, LudoMode.SHADOW, LudoMode.SPIDER, LudoMode.MJOLNIR,
            LudoMode.DEATH_SCYTHE, LudoMode.STORM, LudoMode.REAPER, LudoMode.DRAGON,
            LudoMode.EXCALIBUR, LudoMode.LIGHT_MACE
        };
        return modes[Math.min(index, 9)];
    }

    private LudoMode getRandomMode() {
        LudoMode[] modes = LudoMode.values();
        return modes[random.nextInt(modes.length)];
    }

    private void playModeSound(Player player, LudoMode mode) {
        if (mode == LudoMode.JACKPOT) {
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0f, 1.2f);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void giveOriginalItem(Player player, LudoMode mode) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        playerStats.currentMode = mode;
        
        ItemStack newItem = null;
        
        switch (mode) {
            case FROST:
                newItem = FrostSwordItem.createFrostSword();
                break;
            case SHADOW:
                newItem = ShadowSwordItem.createShadowSword();
                break;
            case SPIDER:
                newItem = SpiderBladeItem.createBlade();
                break;
            case MJOLNIR:
                newItem = MjolnirItem.createMjolnir();
                break;
            case DEATH_SCYTHE:
                newItem = DeathScytheItem.createScythe();
                break;
            case STORM:
                newItem = StormBladeItem.createBlade();
                break;
            case REAPER:
                newItem = ReaperScytheItem.createScythe();
                break;
            case DRAGON:
                newItem = KatanaItem.createKatana();
                break;
            case EXCALIBUR:
                newItem = ExcaliburItem.createExcalibur();
                break;
            case LIGHT_MACE:
                newItem = LightMaceItem.createMace();
                break;
            case JACKPOT:
                newItem = ExcaliburItem.createExcalibur();
                ItemMeta meta = newItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(net.kyori.adventure.text.Component.text("§d§lДЖЕКПОТ"));
                    newItem.setItemMeta(meta);
                }
                break;
        }
        
        if (newItem != null) {
            player.getInventory().setItem(playerStats.slot, newItem);
            player.sendMessage("§aВы получили " + MODE_NAMES.get(mode) + " на " + ITEM_DURATION + " секунд!");
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                returnToLudoSword(player);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ITEM_DURATION * 20L);
    }

    private void returnToLudoSword(Player player) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        if (playerStats == null || playerStats.currentMode == null) return;
        
        LudoMode mode = playerStats.currentMode;
        
        if (playerStats.originalItem != null) {
            player.getInventory().setItem(playerStats.slot, playerStats.originalItem);
            player.sendMessage("§c" + MODE_NAMES.get(mode) + " исчез. Лудо-меч вернулся!");
        }
        
        playerStats.currentMode = null;
        playerStats.cooldownEndTime = System.currentTimeMillis() + (COOLDOWN_DURATION * 1000L);
        
        player.sendMessage("§6Лудо-меч перезаряжается " + COOLDOWN_DURATION + " секунд.");
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лудо-меч");
    }
}