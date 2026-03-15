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
    private final Map<UUID, BubbleInfo> activeBubbles = new HashMap<>();
    
    private static final long COOLDOWN = 90 * 1000; // 90 секунд
    private static final int BUBBLE_DURATION = 7 * 20; // 7 секунд в тиках
    private static final int BUBBLE_SIZE = 7; // 7×7×7
    private static final int FREEZE_TICKS = 140; // 7 секунд заморозки

    private static class BubbleInfo {
        Location center;
        BukkitRunnable visualTask;
        long endTime;

        BubbleInfo(Location center, BukkitRunnable visualTask, long endTime) {
            this.center = center;
            this.visualTask = visualTask;
            this.endTime = endTime;
        }
    }

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
                    if (mob.hasAI()) {
                        mob.setAI(false);
                        
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
            
            // 4. ВИЗУАЛ КУБА (ТОЛЬКО РЁБРА)
            BukkitRunnable visualTask = drawBubbleOutline(center, world);
            
            // Сохраняем информацию о пузыре
            activeBubbles.put(player.getUniqueId(), new BubbleInfo(center, visualTask, now + (BUBBLE_DURATION * 50L)));
            
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
                    return;
                }
                
                for (Entity e : center.getWorld().getEntities()) {
                    if (e instanceof Projectile && isInBubble(e.getLocation(), center)) {
                        e.setVelocity(new Vector(0, 0, 0));
                        frozenProjectiles.add(e.getUniqueId());
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private BukkitRunnable drawBubbleOutline(Location center, World world) {
        double half = BUBBLE_SIZE / 2.0;
        
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    // Останавливаем визуал
                    this.cancel();
                    
                    // Очищаем все активные пузыри этого мира
                    activeBubbles.entrySet().removeIf(entry -> {
                        if (entry.getValue().center.getWorld().equals(world)) {
                            return true;
                        }
                        return false;
                    });
                    return;
                }
                
                // Рисуем только рёбра куба (желтые линии)
                // Нижние рёбра
                drawLine(center.clone().add(-half, -half, -half), center.clone().add(half, -half, -half), world);
                drawLine(center.clone().add(half, -half, -half), center.clone().add(half, -half, half), world);
                drawLine(center.clone().add(half, -half, half), center.clone().add(-half, -half, half), world);
                drawLine(center.clone().add(-half, -half, half), center.clone().add(-half, -half, -half), world);
                
                // Верхние рёбра
                drawLine(center.clone().add(-half, half, -half), center.clone().add(half, half, -half), world);
                drawLine(center.clone().add(half, half, -half), center.clone().add(half, half, half), world);
                drawLine(center.clone().add(half, half, half), center.clone().add(-half, half, half), world);
                drawLine(center.clone().add(-half, half, half), center.clone().add(-half, half, -half), world);
                
                // Вертикальные рёбра
                drawLine(center.clone().add(-half, -half, -half), center.clone().add(-half, half, -half), world);
                drawLine(center.clone().add(half, -half, -half), center.clone().add(half, half, -half), world);
                drawLine(center.clone().add(half, -half, half), center.clone().add(half, half, half), world);
                drawLine(center.clone().add(-half, -half, half), center.clone().add(-half, half, half), world);
                
                ticks++;
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        return task;
    }

    private void drawLine(Location start, Location end, World world) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d <= distance; d += 0.5) {
            Vector step = direction.clone().multiply(d);
            Location point = start.clone().add(step);
            
            // Жёлтые частицы (END_ROD даёт жёлтое свечение)
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile p = event.getEntity();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isDead() || p.isOnGround()) {
                    this.cancel();
                    return;
                }
                
                for (BubbleInfo bubble : activeBubbles.values()) {
                    if (isInBubble(p.getLocation(), bubble.center)) {
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
        frozenProjectiles.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteractEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.getType() == Material.ENDER_PEARL) {
            for (BubbleInfo bubble : activeBubbles.values()) {
                if (isInBubble(player.getLocation(), bubble.center)) {
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