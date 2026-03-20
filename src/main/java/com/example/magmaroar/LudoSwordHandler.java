package com.example.magmaroar;

import net.kyori.adventure.text.Component;
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
        long cooldownEndTime = 0;
        boolean isRolling = false;
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

        // Проверка через CustomModelData 1004
        if (!isLudoSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long now = System.currentTimeMillis();
            LudoStats playerStats = stats.computeIfAbsent(player.getUniqueId(), k -> new LudoStats());
            
            if (playerStats.cooldownEndTime > now) {
                player.sendMessage("§cКулдаун: " + (playerStats.cooldownEndTime - now) / 1000 + " сек.");
                event.setCancelled(true);
                return;
            }
            
            if (playerStats.currentMode != null || playerStats.isRolling) {
                event.setCancelled(true);
                return;
            }
            
            playerStats.slot = player.getInventory().getHeldItemSlot();
            playerStats.isRolling = true;
            
            startRoulette(player);
            event.setCancelled(true);
        }
    }

    private void startRoulette(Player player) {
        player.sendMessage("§6§l🔄 РУЛЕТКА КРУТИТСЯ...");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    LudoStats ps = stats.get(player.getUniqueId());
                    if (ps != null) ps.isRolling = false;
                    
                    LudoMode selected = selectRandomMode();
                    giveOriginalItem(player, selected);
                    this.cancel();
                    return;
                }
                if (ticks % 5 == 0) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void giveOriginalItem(Player player, LudoMode mode) {
        LudoStats ps = stats.get(player.getUniqueId());
        ps.currentMode = mode;
        
        ItemStack newItem = null;
        switch (mode) {
            case FROST: newItem = FrostSwordItem.createFrostSword(); break;
            case SHADOW: newItem = ShadowSwordItem.createShadowSword(); break;
            case SPIDER: newItem = SpiderBladeItem.createBlade(); break;
            case MJOLNIR: newItem = MjolnirItem.createMjolnir(); break;
            case DEATH_SCYTHE: newItem = DeathScytheItem.createScythe(); break;
            case STORM: newItem = StormBladeItem.createBlade(); break;
            case REAPER: newItem = ReaperScytheItem.createScythe(); break;
            case DRAGON: newItem = KatanaItem.createKatana(); break;
            case EXCALIBUR: newItem = ExcaliburItem.createExcalibur(); break;
            case LIGHT_MACE: newItem = LightMaceItem.createMace(); break;
            case JACKPOT:
                newItem = ExcaliburItem.createExcalibur();
                ItemMeta m = newItem.getItemMeta();
                m.displayName(Component.text("§d§lДЖЕКПОТ"));
                newItem.setItemMeta(m);
                break;
        }
        
        if (newItem != null) {
            player.getInventory().setItem(ps.slot, newItem);
            player.sendMessage("§aВыпало: " + MODE_NAMES.get(mode));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                returnToLudoSword(player);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), ITEM_DURATION * 20L);
    }

    private void returnToLudoSword(Player player) {
        LudoStats ps = stats.get(player.getUniqueId());
        if (ps == null || ps.currentMode == null) return;
        
        // Возвращаем предмет с моделью 1004
        player.getInventory().setItem(ps.slot, LudoSwordItem.createSword());
        player.sendMessage("§cЛудо-меч вернулся!");
        
        ps.currentMode = null;
        ps.cooldownEndTime = System.currentTimeMillis() + (COOLDOWN_DURATION * 1000L);
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        // ПРОВЕРКА ПО МОДЕЛИ 1004
        return meta.hasCustomModelData() && meta.getCustomModelData() == 1004;
    }

    private LudoMode selectRandomMode() {
        LudoMode[] modes = LudoMode.values();
        return modes[random.nextInt(modes.length)];
    }
}