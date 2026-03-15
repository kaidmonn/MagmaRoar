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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TimeClockHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BubbleInfo> activeBubbles = new HashMap<>();
    private final Map<UUID, List<FrozenProjectile>> frozenProjectiles = new HashMap<>();
    private final Map<UUID, Long> frozenEntities = new HashMap<>(); // Для отслеживания замороженных
    
    private static final long COOLDOWN = 90 * 1000;
    private static final int BUBBLE_DURATION = 7 * 20;
    private static final int BUBBLE_SIZE = 7;
    private static final int FREEZE_TICKS = 140;

    private static class FrozenProjectile {
        Entity projectile;
        Vector originalVelocity;
        Location frozenLocation;

        FrozenProjectile(Entity projectile, Vector originalVelocity, Location frozenLocation) {
            this.projectile = projectile;
            this.originalVelocity = originalVelocity;
            this.frozenLocation = frozenLocation;
        }
    }

    private static class BubbleInfo {
        Location center;
        BukkitRunnable visualTask;
        BukkitRunnable projectileTask;
        BukkitRunnable checkTask; // Новый таск для проверки входящих
        UUID ownerId;
        long endTime;

        BubbleInfo(Location center, BukkitRunnable visualTask, BukkitRunnable projectileTask, BukkitRunnable checkTask, UUID ownerId, long endTime) {
            this.center = center;
            this.visualTask = visualTask;
            this.projectileTask = projectileTask;
            this.checkTask = checkTask;
            this.ownerId = ownerId;
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
                player.sendMessage("§cЧасы времени перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location center = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);
            World world = player.getWorld();
            
            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            player.sendMessage("§6Часы времени создают временной пузырь!");
            
            // Замораживаем всех, кто уже внутри
            freezeAllInside(center, player);
            
            // Создаём список для замороженных снарядов
            List<FrozenProjectile> projectilesInBubble = new ArrayList<>();
            frozenProjectiles.put(player.getUniqueId(), projectilesInBubble);
            
            // Запускаем проверку снарядов
            BukkitRunnable projectileTask = startProjectileChecker(player, center, projectilesInBubble);
            
            // Запускаем проверку входящих существ
            BukkitRunnable checkTask = startEntityChecker(center, player);
            
            // Визуал куба (жёлтые рёбра)
            BukkitRunnable visualTask = drawBubbleOutline(center, world);
            
            // Сохраняем информацию о пузыре
            activeBubbles.put(player.getUniqueId(), new BubbleInfo(
                center, visualTask, projectileTask, checkTask, player.getUniqueId(), 
                System.currentTimeMillis() + (BUBBLE_DURATION * 50L)
            ));
            
            cooldowns.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    private void freezeAllInside(Location center, Player owner) {
        World world = center.getWorld();
        
        // Замораживаем игроков
        for (Player p : world.getPlayers()) {
            if (!p.equals(owner) && isInBubble(p.getLocation(), center)) {
                freezeEntity(p);
                p.sendMessage("§cВы попали во временной пузырь! Всё замерло...");
            }
        }
        
        // Замораживаем мобов
        for (Entity e : world.getEntities()) {
            if (e instanceof Mob && !e.equals(owner) && isInBubble(e.getLocation(), center)) {
                freezeEntity((LivingEntity) e);
            }
        }
    }

    private BukkitRunnable startEntityChecker(Location center, Player owner) {
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    // Размораживаем всех
                    for (UUID id : frozenEntities.keySet()) {
                        Entity e = findEntity(id);
                        if (e instanceof Player) {
                            Player p = (Player) e;
                            p.setFreezeTicks(0);
                            p.sendMessage("§aВременной пузырь исчез, вы снова можете двигаться!");
                        } else if (e instanceof Mob) {
                            Mob m = (Mob) e;
                            m.setAI(true);
                        }
                    }
                    frozenEntities.clear();
                    this.cancel();
                    return;
                }
                
                // Проверяем новых существ, вошедших в пузырь
                for (Entity e : center.getWorld().getEntities()) {
                    if (e instanceof LivingEntity && !e.equals(owner) && isInBubble(e.getLocation(), center)) {
                        LivingEntity le = (LivingEntity) e;
                        
                        // Если ещё не заморожен
                        if (!frozenEntities.containsKey(e.getUniqueId())) {
                            freezeEntity(le);
                            
                            if (e instanceof Player) {
                                ((Player) e).sendMessage("§cВы вошли во временной пузырь! Всё замерло...");
                            }
                        }
                    }
                }
                
                ticks++;
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L); // Проверка каждые 5 тиков
        return task;
    }

    private void freezeEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            Player p = (Player) entity;
            p.setFreezeTicks(FREEZE_TICKS);
            p.setWalkSpeed(0);
            p.setFlySpeed(0);
            p.setAllowFlight(true);
            p.setFlying(true);
        } else if (entity instanceof Mob) {
            Mob m = (Mob) entity;
            if (m.hasAI()) {
                m.setAI(false);
            }
        }
        
        frozenEntities.put(entity.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Если игрок заморожен, отменяем движение
        if (frozenEntities.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isInBubble(Location loc, Location center) {
        double dx = Math.abs(loc.getX() - center.getX());
        double dy = Math.abs(loc.getY() - center.getY());
        double dz = Math.abs(loc.getZ() - center.getZ());
        
        return dx <= BUBBLE_SIZE/2 && dy <= BUBBLE_SIZE/2 && dz <= BUBBLE_SIZE/2;
    }

    private BukkitRunnable startProjectileChecker(Player player, Location center, List<FrozenProjectile> projectilesInBubble) {
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    // Размораживаем снаряды
                    for (FrozenProjectile fp : projectilesInBubble) {
                        if (!fp.projectile.isDead()) {
                            fp.projectile.setVelocity(fp.originalVelocity);
                        }
                    }
                    projectilesInBubble.clear();
                    frozenProjectiles.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Проверяем новые снаряды
                for (Entity e : center.getWorld().getEntities()) {
                    if (e instanceof Projectile && isInBubble(e.getLocation(), center)) {
                        boolean alreadyFrozen = false;
                        for (FrozenProjectile fp : projectilesInBubble) {
                            if (fp.projectile.equals(e)) {
                                alreadyFrozen = true;
                                break;
                            }
                        }
                        
                        if (!alreadyFrozen) {
                            Vector vel = e.getVelocity().clone();
                            Location loc = e.getLocation().clone();
                            e.setVelocity(new Vector(0, 0, 0));
                            projectilesInBubble.add(new FrozenProjectile(e, vel, loc));
                        }
                    }
                }
                
                ticks++;
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        return task;
    }

    private BukkitRunnable drawBubbleOutline(Location center, World world) {
        double half = BUBBLE_SIZE / 2.0;
        
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BUBBLE_DURATION) {
                    this.cancel();
                    return;
                }
                
                Particle.DustOptions yellowDust = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(255, 215, 0), 1.5f
                );
                
                // Рисуем рёбра куба
                drawLine(center.clone().add(-half, -half, -half), center.clone().add(half, -half, -half), world, yellowDust);
                drawLine(center.clone().add(half, -half, -half), center.clone().add(half, -half, half), world, yellowDust);
                drawLine(center.clone().add(half, -half, half), center.clone().add(-half, -half, half), world, yellowDust);
                drawLine(center.clone().add(-half, -half, half), center.clone().add(-half, -half, -half), world, yellowDust);
                
                drawLine(center.clone().add(-half, half, -half), center.clone().add(half, half, -half), world, yellowDust);
                drawLine(center.clone().add(half, half, -half), center.clone().add(half, half, half), world, yellowDust);
                drawLine(center.clone().add(half, half, half), center.clone().add(-half, half, half), world, yellowDust);
                drawLine(center.clone().add(-half, half, half), center.clone().add(-half, half, -half), world, yellowDust);
                
                drawLine(center.clone().add(-half, -half, -half), center.clone().add(-half, half, -half), world, yellowDust);
                drawLine(center.clone().add(half, -half, -half), center.clone().add(half, half, -half), world, yellowDust);
                drawLine(center.clone().add(half, -half, half), center.clone().add(half, half, half), world, yellowDust);
                drawLine(center.clone().add(-half, -half, half), center.clone().add(-half, half, half), world, yellowDust);
                
                ticks++;
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        return task;
    }

    private void drawLine(Location start, Location end, World world, Particle.DustOptions color) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d <= distance; d += 0.3) {
            Vector step = direction.clone().multiply(d);
            Location point = start.clone().add(step);
            
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, color);
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
                        Vector vel = p.getVelocity().clone();
                        p.setVelocity(new Vector(0, 0, 0));
                        
                        List<FrozenProjectile> projectiles = frozenProjectiles.get(bubble.ownerId);
                        if (projectiles != null) {
                            boolean alreadyFrozen = false;
                            for (FrozenProjectile fp : projectiles) {
                                if (fp.projectile.equals(p)) {
                                    alreadyFrozen = true;
                                    break;
                                }
                            }
                            if (!alreadyFrozen) {
                                projectiles.add(new FrozenProjectile(p, vel, p.getLocation().clone()));
                            }
                        }
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
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

    private Entity findEntity(UUID id) {
        for (World world : MagmaRoarPlugin.getInstance().getServer().getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(id)) {
                    return e;
                }
            }
        }
        return null;
    }

    private boolean isTimeClock(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Часы времени");
    }
}