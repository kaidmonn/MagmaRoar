package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TimeClockHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> frozenProjectiles = new HashSet<>();
    private final Map<UUID, Location> bubbleLocations = new HashMap<>();
    
    private static final long COOLDOWN = 90 * 1000; // 90 секунд
    private static final int BUBBLE_DURATION = 7 * 20; // 7 секунд в тиках
    private static final int BUBBLE_SIZE = 7; // 7×7×7
    private static final int FREEZE_TICKS = 140; // 7 секунд заморозки

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isTimeClock(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЧасы времени перезаряжаются! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Получаем точку взгляда
            Location center = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);
            World world = player.getWorld();
            
            // Сохраняем центр пузыря
            bubbleLocations.put(player.getUniqueId(), center);
            
            // Звук активации
            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§6Часы времени создают временной пузырь!");
            
            // 1. ЗАМОРАЖИВАЕМ ИГРОКОВ
            for (Player p : world.getPlayers()) {
                if (!p.equals(player) && isInBubble(p.getLocation(), center)) {
                    p.setFreezeTicks(FREEZE_TICKS);
                    p.sendMessage("§cВы попали во временной пузырь! Всё замерло...");
                }
            }
            
            // 2. ОСТАНАВЛИВАЕМ МОБОВ
            for (Entity e : world.getEntities()) {
                if (e instanceof Mob && !e.equals(player) && isInBubble(e.getLocation(), center)) {
                    Mob mob = (Mob) e;
                    // Запоминаем состояние ИИ
                    if (mob.hasAI()) {
                        mob.setAI(false);
                        
                        // Возвращаем ИИ через 7 секунд
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!mob.isDead()) {
                                    mob.setAI(true);
                                }
                            }
                        }.runTaskLater(MagmaRoarPlugin.getInstance(), BUBBLE_DURATION);
                    }
                }
            }
            
            // 3. ЗАПУСКАЕМ ПРОВЕРКУ СНАРЯДОВ
            startProjectileChecker(player, center);
            
            // РИСУЕМ КОНТУР ПУЗЫРЯ
            drawBubbleOutline(center, world, player);
            
            cooldowns.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    private boolean isInBubble(Location loc, Location center) {
        double dx = Math.abs(loc.getX() - center.getX());
        double dy = Math.abs(loc.getY() - center.getY());
        double dz = Math.abs(loc.getZ() - center.getZ());
        
        return dx <= BUBBLE_SIZE/2 && dy <= BUBBLE_SIZE/2 && dz <= BUBBLE_SIZE/2;
    }

    private void startProjectileChecker(Player player, Location center) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    this.cancel();
                    bubbleLocations.remove(player.getUniqueId());
                    return;
                }
                
                // Проверяем все снаряды в мире
                for (Entity e : center.getWorld().getEntities()) {
                    if (e instanceof Projectile && isInBubble(e.getLocation(), center)) {
                        // Замораживаем снаряд
                        e.setVelocity(new Vector(0, 0, 0));
                        frozenProjectiles.add(e.getUniqueId());
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void drawBubbleOutline(Location center, World world, Player owner) {
        double half = BUBBLE_SIZE / 2.0;
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    this.cancel();
                    return;
                }
                
                // Рисуем рёбра куба
                for (double x = -half; x <= half; x += 0.5) {
                    for (double y = -half; y <= half; y += 0.5) {
                        for (double z = -half; z <= half; z += 0.5) {
                            // Только на рёбрах
                            if (Math.abs(x) == half || Math.abs(y) == half || Math.abs(z) == half) {
                                if (Math.abs(x) == half || Math.abs(y) == half || Math.abs(z) == half) {
                                    Location particleLoc = center.clone().add(x, y, z);
                                    owner.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Запоминаем, что снаряд летит
        Projectile p = event.getEntity();
        
        // Проверяем, не попадёт ли он в пузырь
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isDead() || p.isOnGround()) {
                    this.cancel();
                    return;
                }
                
                // Проверяем все активные пузыри
                for (Map.Entry<UUID, Location> entry : bubbleLocations.entrySet()) {
                    if (isInBubble(p.getLocation(), entry.getValue())) {
                        p.setVelocity(new Vector(0, 0, 0));
                        frozenProjectiles.add(p.getUniqueId());
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Убираем замороженные снаряды из памяти
        frozenProjectiles.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteractEnderPearl(PlayerInteractEvent event) {
        // Блокируем эндер-жемчуг в пузыре
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.getType() == Material.ENDER_PEARL) {
            for (Map.Entry<UUID, Location> entry : bubbleLocations.entrySet()) {
                if (!entry.getKey().equals(player.getUniqueId()) && 
                    isInBubble(player.getLocation(), entry.getValue())) {
                    player.sendMessage("§cЭндер-жемчуг не работает во временном пузыре!");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isTimeClock(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Часы времени");
    }
}