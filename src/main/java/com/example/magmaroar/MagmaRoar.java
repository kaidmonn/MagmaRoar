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
    private static final long JUMP_COOLDOWN = 1000; // 1 секунда
    private static final double JUMP_HEIGHT = 0.45; // 3 блока
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
                
                // Отключаем ИИ
                this.strider.setAI(false);
                
                // Надеваем седло
                this.strider.setSaddle(true);
                
                // Атрибуты
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
                
                // Эффекты
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

                // Поджигаем блок под каждым блоком страйдера (все 4 ноги)
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (Math.abs(x) + Math.abs(z) <= 1) { // Только соседние блоки (крест)
                            Location footLoc = strider.getLocation().add(x, -1, z);
                            if (footLoc.getBlock().getType() == Material.AIR || 
                                footLoc.getBlock().getType() == Material.FIRE) {
                                footLoc.getBlock().setType(Material.FIRE);
                            }
                        }
                    }
                }
                
                // Частицы
                strider.getWorld().spawnParticle(org.bukkit.Particle.FLAME, 
                    strider.getLocation().add(0, 1, 0), 5, 0.8, 0.5, 0.8, 0.02);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 3L); // Чаще, для лучшего эффекта

        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    cleanup();
                    return;
                }

                if (isRiding && owner.isOnline() && owner.getVehicle() != null && 
                    owner.getVehicle().equals(strider)) {
                    
                    // УПРАВЛЕНИЕ БЕЗ УДОЧКИ
                    // Получаем направление взгляда игрока
                    Vector direction = owner.getLocation().getDirection().normalize();
                    
                    // Сохраняем текущую вертикальную скорость
                    double currentY = strider.getVelocity().getY();
                    
                    // Горизонтальное движение (всегда активно)
                    Vector velocity = new Vector(direction.getX() * 0.8, currentY, direction.getZ() * 0.8);
                    strider.setVelocity(velocity);
                    
                    // Поворот страйдера
                    Location loc = strider.getLocation();
                    loc.setYaw(owner.getLocation().getYaw());
                    loc.setPitch(0);
                    strider.teleport(loc);
                    
                    // Если игрок нажимает Shift - замедляем падение
                    if (owner.isSneaking() && currentY < -0.1) {
                        strider.setVelocity(new Vector(velocity.getX(), currentY * 0.5, velocity.getZ()));
                    }
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
                        // Отключаем ванильный полёт
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

        // Прыжок на 3 блока
        Vector currentVel = strider.getVelocity();
        strider.setVelocity(currentVel.add(new Vector(0, JUMP_HEIGHT, 0)));
        
        // Звук
        strider.getWorld().playSound(strider.getLocation(), org.bukkit.Sound.ENTITY_HORSE_JUMP, 1.0f, 1.0f);
        
        // Поджигаем ВСЕ блоки под страйдером (3x3)
        Location jumpLoc = strider.getLocation().subtract(0, 1, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue; // Пропускаем центр (уже горит)
                Location fireLoc = jumpLoc.clone().add(x, 0, z);
                if (fireLoc.getBlock().getType() == Material.AIR || 
                    fireLoc.getBlock().getType() == Material.FIRE) {
                    fireLoc.getBlock().setType(Material.FIRE);
                }
            }
        }
        
        // Частицы
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
        
        // ИСПОЛЬЗУЕМ ФАЙЕРБОЛЛ (работает стабильно)
        Fireball projectile = world.spawn(spawnLoc, Fireball.class);
        projectile.setVelocity(direction.multiply(2.0));
        projectile.setYield(4.0f); // Сила взрыва
        projectile.setIsIncendiary(false); // Не поджигает блоки
        projectile.setShooter(owner);
        projectile.setDirection(direction);
        
        // Делаем файерболл меньше и быстрее
        projectile.setVisualFire(false);
        
        world.playSound(spawnLoc, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        // Отслеживаем взрыв
        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile == null || projectile.isDead() || projectile.isOnGround()) {
                    
                    if (projectile != null && !projectile.isDead()) {
                        Location hitLoc = projectile.getLocation();
                        
                        // Звук взрыва
                        world.playSound(hitLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        
                        // Частицы удара булавы
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
            owner.sendMessage("§aВы оседлали Магма Рёва!");
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