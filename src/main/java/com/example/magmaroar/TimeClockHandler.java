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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
    private final Set<UUID> frozenEntities = new HashSet<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>();
    
    private static final long COOLDOWN = 90 * 1000;
    private static final int BUBBLE_DURATION = 7 * 20;
    private static final int BUBBLE_SIZE = 7;
    private static final int FREEZE_DURATION = 7 * 1000; // 7 секунд

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
        BukkitRunnable checkTask;
        UUID ownerId;
        long endTime;
        boolean isActive;

        BubbleInfo(Location center, BukkitRunnable visualTask, BukkitRunnable projectileTask, BukkitRunnable checkTask, UUID ownerId, long endTime) {
            this.center = center;
            this.visualTask = visualTask;
            this.projectileTask = projectileTask;
            this.checkTask = checkTask;
            this.ownerId = ownerId;
            this.endTime = endTime;
            this.isActive = true;
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
            BubbleInfo bubbleInfo = new BubbleInfo(
                center, visualTask, projectileTask, checkTask, player.getUniqueId(), 
                System.currentTimeMillis() + (BUBBLE_DURATION * 50L)
            );
            activeBubbles.put(player.getUniqueId(), bubbleInfo);
            
            // Таймер на удаление пузыря
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeBubble(player.getUniqueId());
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), BUBBLE_DURATION);
            
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
                p.sendMessage("§c§lВЫ ВО ВРЕМЕННОМ ПУЗЫРЕ!");
                p.sendMessage("§cВы не можете двигаться, атаковать, ставить блоки или стрелять.");
                p.sendMessage("§eМожно только есть и вертеть камерой.");
            }
        }
        
        // Замораживаем мобов
        for (Entity e : world.getEntities()) {
            if (e instanceof Mob && !e.equals(owner) && isInBubble(e.getLocation(), center)) {
                freezeEntity((LivingEntity) e);
            }
        }
    }

    private void freezeEntity(LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        frozenEntities.add(entityId);
        frozenUntil.put(entityId, System.currentTimeMillis() + FREEZE_DURATION);
        
        if (entity instanceof Player) {
            Player p = (Player) entity;
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
    }

    private void unfreezeEntity(Entity entity) {
        if (entity == null || entity.isDead()) return;
        
        UUID entityId = entity.getUniqueId();
        frozenEntities.remove(entityId);
        frozenUntil.remove(entityId);
        
        if (entity instanceof Player) {
            Player p = (Player) entity;
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.sendMessage("§aВременной пузырь исчез, вы снова можете двигаться!");
        } else if (entity instanceof Mob) {
            Mob m = (Mob) entity;
            m.setAI(true);
        }
    }

    private boolean isFrozen(UUID entityId) {
        Long until = frozenUntil.get(entityId);
        return until != null && System.currentTimeMillis() < until;
    }

    private BukkitRunnable startEntityChecker(Location center, Player owner) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                BubbleInfo bubble = activeBubbles.get(owner.getUniqueId());
                if (bubble == null || !bubble.isActive) {
                    this.cancel();
                    return;
                }
                
                // Проверяем новых существ, вошедших в пузырь
                for (Entity e : center.getWorld().getEntities()) {
                    if (e instanceof LivingEntity && !e.equals(owner) && isInBubble(e.getLocation(), center)) {
                        LivingEntity le = (LivingEntity) e;
                        
                        // Если ещё не заморожен
                        if (!frozenEntities.contains(e.getUniqueId())) {
                            freezeEntity(le);
                            
                            if (e instanceof Player) {
                                ((Player) e).sendMessage("§c§lВЫ ВОШЛИ ВО ВРЕМЕННОЙ ПУЗЫРЬ!");
                                ((Player) e).sendMessage("§cВы не можете двигаться, атаковать, ставить блоки или стрелять.");
                                ((Player) e).sendMessage("§eМожно только есть и вертеть камерой.");
                            }
                        }
                    }
                }
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
        return task;
    }

    private void removeBubble(UUID ownerId) {
        BubbleInfo bubble = activeBubbles.remove(ownerId);
        if (bubble == null) return;
        
        bubble.isActive = false;
        if (bubble.visualTask != null) bubble.visualTask.cancel();
        if (bubble.projectileTask != null) bubble.projectileTask.cancel();
        if (bubble.checkTask != null) bubble.checkTask.cancel();
        
        // Размораживаем всех
        for (UUID entityId : new HashSet<>(frozenEntities)) {
            Entity e = findEntity(entityId);
            unfreezeEntity(e);
        }
        frozenEntities.clear();
        frozenUntil.clear();
        
        // Размораживаем снаряды
        List<FrozenProjectile> projectiles = frozenProjectiles.remove(ownerId);
        if (projectiles != null) {
            for (FrozenProjectile fp : projectiles) {
                if (!fp.projectile.isDead()) {
                    fp.projectile.setVelocity(fp.originalVelocity);
                }
            }
        }
    }

    // ==================== БЛОКИРОВКА ДЕЙСТВИЙ ====================

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            event.setCancelled(true); // Не может двигаться
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (isFrozen(damager.getUniqueId())) {
                event.setCancelled(true); // Не может бить
                damager.sendMessage("§cВы заморожены во времени и не можете атаковать!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cВы заморожены во времени и не можете ломать блоки!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cВы заморожены во времени и не можете ставить блоки!");
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§cВы заморожены во времени и не можете стрелять!");
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            if (isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§cВы заморожены во времени и не можете кидать снаряды!");
            }
        }

        Projectile p = event.getEntity();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isDead() || p.isOnGround()) {
                    this.cancel();
                    return;
                }
                
                for (BubbleInfo bubble : activeBubbles.values()) {
                    if (bubble.isActive && isInBubble(p.getLocation(), bubble.center)) {
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
            if (isFrozen(player.getUniqueId())) {
                player.sendMessage("§cВы заморожены во времени и не можете использовать эндер-жемчуг!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        // Есть можно даже замороженным
        if (isFrozen(player.getUniqueId())) {
            player.sendMessage("§aВы едите, несмотря на заморозку времени...");
        }
    }

    // ==================== ПРОЧЕЕ ====================

    private boolean isInBubble(Location loc, Location center) {
        for (BubbleInfo bubble : activeBubbles.values()) {
            if (!bubble.isActive) continue;
            
            double dx = Math.abs(loc.getX() - bubble.center.getX());
            double dy = Math.abs(loc.getY() - bubble.center.getY());
            double dz = Math.abs(loc.getZ() - bubble.center.getZ());
            
            if (dx <= BUBBLE_SIZE/2 && dy <= BUBBLE_SIZE/2 && dz <= BUBBLE_SIZE/2) {
                return true;
            }
        }
        return false;
    }

    private BukkitRunnable startProjectileChecker(Player player, Location center, List<FrozenProjectile> projectilesInBubble) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                BubbleInfo bubble = activeBubbles.get(player.getUniqueId());
                if (bubble == null || !bubble.isActive) {
                    this.cancel();
                    return;
                }
                
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
            }
        };
        
        task.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        return task;
    }

    private BukkitRunnable drawBubbleOutline(Location center, World world) {
        double half = BUBBLE_SIZE / 2.0;
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                BubbleInfo bubble = activeBubbles.values().stream()
                    .filter(b -> b.center.equals(center))
                    .findFirst()
                    .orElse(null);
                    
                if (bubble == null || !bubble.isActive) {
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