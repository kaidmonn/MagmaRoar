package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MagmaRoar {

    private final Player owner;
    private Strider strider;
    private boolean isSummoned = false;
    private boolean isRiding = false;
    private long lastAttackTime = 0;
    private long lastJumpTime = 0;
    private long summonTime = 0;
    private static final long ATTACK_COOLDOWN = 20 * 1000;
    private static final long JUMP_COOLDOWN = 1000;
    private static final double JUMP_HEIGHT = 0.45;
    private static final long DESPAWN_TIME = 90 * 1000;
    private static final long SUMMON_COOLDOWN = 3 * 60 * 1000;
    private BukkitTask despawnTask;
    private BukkitTask movementTask;
    private BukkitTask fireTrailTask;
    private BukkitTask passengerCheckTask;

    public static final Map<UUID, MagmaRoar> activeMagmaRoars = new HashMap<>();
    public static final Map<UUID, Long> lastSummonTime = new HashMap<>();

    public MagmaRoar(Player owner, Location location) {
        this.owner = owner;
        this.summonTime = System.currentTimeMillis();

        World world = location.getWorld();
        if (world != null) {
            Entity entity = world.spawnEntity(location, EntityType.STRIDER);
            if (entity instanceof Strider) {
                this.strider = (Strider) entity;
                
                this.strider.setAI(false);
                this.strider.setSaddle(true);
                
                AttributeInstance scaleAttr = this.strider.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) scaleAttr.setBaseValue(2.0);
                
                AttributeInstance speedAttr = this.strider.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speedAttr != null) speedAttr.setBaseValue(0.5);
                
                AttributeInstance healthAttr = this.strider.getAttribute(Attribute.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(48);
                    this.strider.setHealth(48);
                }
                
                AttributeInstance knockbackAttr = this.strider.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (knockbackAttr != null) knockbackAttr.setBaseValue(1.0);
                
                this.strider.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                this.strider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 5, false, false));

                this.isSummoned = true;
                activeMagmaRoars.put(owner.getUniqueId(), this);
                lastSummonTime.put(owner.getUniqueId(), summonTime);

                world.playSound(location, org.bukkit.Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);

                startTasks();
                startDespawnTimer();
                
                owner.sendMessage("§aМагма Рёв призван! Он исчезнет через 90 секунд.");
            } else {
                owner.sendMessage("§cНе удалось создать Магма Рёва!");
            }
        } else {
            owner.sendMessage("§cМир не найден!");
        }
    }

    private void startTasks() {
        if (strider == null) return;
        
        fireTrailTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    cleanup();
                    return;
                }

                // Поджигаем область под страйдером
                Location footLoc = strider.getLocation().subtract(0, 1, 0);
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location fireLoc = footLoc.clone().add(x, 0, z);
                        if (fireLoc.getBlock().getType() == Material.AIR || 
                            fireLoc.getBlock().getType() == Material.FIRE) {
                            fireLoc.getBlock().setType(Material.FIRE);
                        }
                    }
                }
                
                strider.getWorld().spawnParticle(org.bukkit.Particle.FLAME, 
                    strider.getLocation().add(0, 1, 0), 5, 0.8, 0.5, 0.8, 0.02);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 3L);

        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    cleanup();
                    return;
                }

                if (isRiding && owner.isOnline() && owner.getVehicle() != null && 
                    owner.getVehicle().equals(strider)) {
                    
                    // WASD УПРАВЛЕНИЕ через направление взгляда
                    Vector direction = owner.getLocation().getDirection().normalize();
                    
                    // Сохраняем вертикальную скорость для гравитации
                    double currentY = strider.getVelocity().getY();
                    
                    // Горизонтальное движение
                    Vector velocity = new Vector(direction.getX() * 0.8, currentY, direction.getZ() * 0.8);
                    strider.setVelocity(velocity);
                    
                    // Поворот страйдера
                    Location loc = strider.getLocation();
                    loc.setYaw(owner.getLocation().getYaw());
                    loc.setPitch(0);
                    strider.teleport(loc);
                    
                    // Отладка (можно убрать)
                    // owner.sendMessage("§7Скорость: " + String.format("%.2f", velocity.length()));
                }

                if (isSummoned && !isRiding && owner.isOnline()) {
                    if (owner.getLocation().distance(strider.getLocation()) > 10) {
                        Vector toPlayer = owner.getLocation().toVector().subtract(strider.getLocation().toVector());
                        double currentY = strider.getVelocity().getY();
                        Vector velocity = toPlayer.normalize().multiply(0.5);
                        velocity.setY(currentY);
                        strider.setVelocity(velocity);
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);

        passengerCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    return;
                }

                if (!strider.getPassengers().isEmpty() && strider.getPassengers().get(0) instanceof Player) {
                    Player rider = (Player) strider.getPassengers().get(0);
                    
                    if (!rider.equals(owner)) {
                        strider.removePassenger(rider);
                        rider.sendMessage("§cВы не можете сесть на чужого Магма Рёва!");
                    } else {
                        isRiding = true;
                        rider.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                        rider.setAllowFlight(false);
                        rider.setFlying(false);
                    }
                } else {
                    isRiding = false;
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 10L);
    }

    public void jump() {
        if (strider == null || strider.isDead()) return;
        if (!isRiding) {
            owner.sendMessage("§cВы должны сидеть на Магма Рёве, чтобы прыгать!");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastJumpTime < JUMP_COOLDOWN) return;

        lastJumpTime = currentTime;

        // Прыжок
        Vector currentVel = strider.getVelocity();
        strider.setVelocity(currentVel.add(new Vector(0, JUMP_HEIGHT, 0)));
        
        strider.getWorld().playSound(strider.getLocation(), org.bukkit.Sound.ENTITY_HORSE_JUMP, 1.0f, 1.0f);
        
        // Огонь при прыжке
        Location jumpLoc = strider.getLocation().subtract(0, 1, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location fireLoc = jumpLoc.clone().add(x, 0, z);
                if (fireLoc.getBlock().getType() == Material.AIR || 
                    fireLoc.getBlock().getType() == Material.FIRE) {
                    fireLoc.getBlock().setType(Material.FIRE);
                }
            }
        }
        
        strider.getWorld().spawnParticle(org.bukkit.Particle.FLAME, strider.getLocation(), 100, 2.5, 1.0, 2.5, 0.1);
        strider.getWorld().spawnParticle(org.bukkit.Particle.LAVA, strider.getLocation(), 50, 2.0, 0.5, 2.0, 0);
    }

    public void attack() {
        if (!isSummoned || strider == null || strider.isDead()) return;
        
        if (!isRiding) {
            owner.sendMessage("§cВы должны сидеть на Магма Рёве, чтобы атаковать!");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < ATTACK_COOLDOWN) {
            long secondsLeft = (ATTACK_COOLDOWN - (currentTime - lastAttackTime)) / 1000;
            owner.sendMessage("§cАтака перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        lastAttackTime = currentTime;

        World world = strider.getWorld();
        Location spawnLoc = strider.getLocation().add(0, 1.5, 0);
        Vector direction = owner.getLocation().getDirection().normalize();
        
        // Файерболл (работает)
        Fireball projectile = world.spawn(spawnLoc, Fireball.class);
        projectile.setVelocity(direction.multiply(2.0));
        projectile.setYield(4.0f);
        projectile.setIsIncendiary(false);
        projectile.setShooter(owner);
        projectile.setDirection(direction);
        projectile.setVisualFire(false);
        
        world.playSound(spawnLoc, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile == null || projectile.isDead() || projectile.isOnGround()) {
                    
                    if (projectile != null && !projectile.isDead()) {
                        Location hitLoc = projectile.getLocation();
                        
                        // Взрыв (блоки не ломаются)
                        world.createExplosion(hitLoc, 4.0f, false, false, strider);
                        world.playSound(hitLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        
                        world.spawnParticle(org.bukkit.Particle.SONIC_BOOM, hitLoc, 30, 2.0, 1.0, 2.0, 0);
                        world.spawnParticle(org.bukkit.Particle.FLAME, hitLoc, 50, 2.0, 1.0, 2.0, 0.1);
                        world.spawnParticle(org.bukkit.Particle.LAVA, hitLoc, 30, 1.5, 1.0, 1.5, 0);
                        
                        projectile.remove();
                    }
                    
                    this.cancel();
                }
                
                if (projectile != null && projectile.getTicksLived() > 100) {
                    if (!projectile.isDead()) {
                        projectile.remove();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        
        owner.sendMessage("§aМагма Рёв выстрелил файерболлом!");
    }

    public void mount(Player player) {
        if (!player.equals(owner)) {
            player.sendMessage("§cЭто не ваш Магма Рёв!");
            return;
        }
        
        if (isSummoned && strider != null && !strider.isDead() && !isRiding) {
            strider.addPassenger(owner);
            isRiding = true;
            owner.setAllowFlight(false);
            owner.setFlying(false);
            owner.sendMessage("§aВы оседлали Магма Рёва! WASD - движение, Пробел - прыжок, ПКМ - атака");
        }
    }

    public void dismount() {
        if (isRiding && owner.getVehicle() != null && owner.getVehicle().equals(strider)) {
            strider.removePassenger(owner);
            isRiding = false;
            owner.sendMessage("§eВы слезли с Магма Рёва.");
        }
    }

    private void startDespawnTimer() {
        despawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isSummoned && strider != null && !strider.isDead()) {
                    owner.sendMessage("§cМагма Рёв исчезает... Время призыва истекло.");
                    strider.getWorld().playSound(strider.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                    cleanup();
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), DESPAWN_TIME / 50);
    }

    private void cleanup() {
        if (strider != null && !strider.isDead()) {
            strider.remove();
        }
        if (movementTask != null) movementTask.cancel();
        if (fireTrailTask != null) fireTrailTask.cancel();
        if (passengerCheckTask != null) passengerCheckTask.cancel();
        if (despawnTask != null) despawnTask.cancel();
        activeMagmaRoars.remove(owner.getUniqueId());
        isSummoned = false;
    }

    public static boolean canSummon(Player player) {
        Long lastSummon = lastSummonTime.get(player.getUniqueId());
        if (lastSummon == null) return true;
        return System.currentTimeMillis() - lastSummon >= SUMMON_COOLDOWN;
    }

    public static long getRemainingCooldown(Player player) {
        Long lastSummon = lastSummonTime.get(player.getUniqueId());
        if (lastSummon == null) return 0;
        long timeSince = System.currentTimeMillis() - lastSummon;
        if (timeSince >= SUMMON_COOLDOWN) return 0;
        return (SUMMON_COOLDOWN - timeSince) / 1000;
    }

    public Strider getStrider() { return strider; }
    public boolean isSummoned() { return isSummoned; }
    public boolean isRiding() { return isRiding; }
}