package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PoseidonTridentHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, UUID> thrownTridents = new HashMap<>();
    private final Map<UUID, ItemStack> returnItems = new HashMap<>();

    private static final long COOLDOWN = 30 * 1000; // 30 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isPoseidonTrident(item)) return;

        // Shift+ПКМ - Рывок Посейдона
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            performRiptide(player, item);
            return;
        }
    }

    private void performRiptide(Player player, ItemStack tridentItem) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN) {
            long secondsLeft = (COOLDOWN - (now - cooldowns.get(uuid))) / 1000;
            player.sendMessage("§cРывок перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        Location loc = player.getLocation();
        World world = player.getWorld();

        returnItems.put(uuid, tridentItem.clone());

        loc.getBlock().setType(Material.WATER);

        player.sendMessage("§b§lРЫВОК ПОСЕЙДОНА!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (loc.getBlock().getType() == Material.WATER) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 1L);

        cooldowns.put(uuid, now);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : world.getEntities()) {
                    if (e instanceof Trident) {
                        Trident trident = (Trident) e;
                        if (trident.getShooter() instanceof Player && ((Player) trident.getShooter()).equals(player)) {
                            thrownTridents.put(trident.getUniqueId(), uuid);
                            
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (trident.isDead() || trident.isOnGround()) {
                                        returnTrident(player, trident.getLocation());
                                        this.cancel();
                                    }
                                }
                            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
                            
                            this.cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 2L);
    }

    private void returnTrident(Player player, Location hitLoc) {
        UUID uuid = player.getUniqueId();
        
        World world = player.getWorld();
        world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION, hitLoc, 20, 2, 1, 2, 0);
        
        for (Entity entity : world.getNearbyEntities(hitLoc, 4, 2, 4)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                ((LivingEntity) entity).damage(5.0, player);
            }
        }
        
        player.sendMessage("§b§lУДАРНАЯ ВОЛНА!");
        
        ItemStack returnItem = returnItems.remove(uuid);
        if (returnItem != null) {
            player.getInventory().addItem(returnItem);
            player.sendMessage("§aТрезубец Посейдона вернулся!");
        }
        
        thrownTridents.values().remove(uuid);
    }

    private boolean isPoseidonTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        return ChatColor.stripColor(meta.getDisplayName()).contains("Трезубец Посейдона");
    }
}