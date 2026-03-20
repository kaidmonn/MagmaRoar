package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PoseidonTridentHandler implements Listener {

    private final Map<UUID, Long> dashCooldowns = new HashMap<>();

    private static final long DASH_COOLDOWN = 8 * 1000; // 8 секунд

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isPoseidonTrident(item)) return;

        // Shift+ПКМ - Рывок Посейдона
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            performDash(player);
            return;
        }
    }

    private void performDash(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (dashCooldowns.containsKey(uuid) && now - dashCooldowns.get(uuid) < DASH_COOLDOWN) {
            long secondsLeft = (DASH_COOLDOWN - (now - dashCooldowns.get(uuid))) / 1000;
            player.sendMessage("§cРывок перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        Location loc = player.getLocation();
        World world = player.getWorld();

        // Ставим воду на 1 тик для активации Riptide
        loc.getBlock().setType(Material.WATER);

        player.sendMessage("§b§lРЫВОК ПОСЕЙДОНА!");

        // Убираем воду через 1 тик
        new BukkitRunnable() {
            @Override
            public void run() {
                if (loc.getBlock().getType() == Material.WATER) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 1L);

        dashCooldowns.put(uuid, now);

        // Запускаем проверку на приземление
        new BukkitRunnable() {
            boolean wasInAir = true;
            @Override
            public void run() {
                if (player.isOnGround() && wasInAir) {
                    createShockwave(player);
                    this.cancel();
                }
                wasInAir = !player.isOnGround();
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void createShockwave(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION, loc, 20, 2, 1, 2, 0);
        // Простые водяные частицы
        world.spawnParticle(Particle.WATER_SPLASH, loc, 50, 2, 1, 2, 0.2);

        for (Entity entity : world.getNearbyEntities(loc, 4, 2, 4)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(5.0, player);
                target.setVelocity(target.getVelocity().add(new Vector(0, 1, 0)));
            }
        }

        player.sendMessage("§b§lУДАРНАЯ ВОЛНА!");
    }

    private boolean isPoseidonTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        return ChatColor.stripColor(meta.getDisplayName()).contains("Трезубец Посейдона");
    }
}