package com.example.dragonstaff;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DragonEntity {

    private final Player owner;
    private EnderDragon dragon;
    private boolean isSummoned = false;
    private boolean isRiding = false;
    private boolean isHovering = false;
    private long lastAttackTime = 0;
    private long summonTime = 0;
    private static final long ATTACK_COOLDOWN = 20 * 1000;
    private static final long DESPAWN_TIME = 90 * 1000;
    private static final long SUMMON_COOLDOWN = 3 * 60 * 1000;
    private BukkitTask despawnTask;
    private BukkitTask movementTask;
    private TNTPrimed tntProjectile; // Для отслеживания снаряда

    public static final Map<UUID, DragonEntity> activeDragons = new HashMap<>();
    public static final Map<UUID, Long> lastSummonTime = new HashMap<>();

    public DragonEntity(Player owner, Location location) {
        this.owner = owner;
        this.summonTime = System.currentTimeMillis();

        World world = location.getWorld();
        if (world != null) {
            // Спавним дракона
            this.dragon = (EnderDragon) world.spawnEntity(location, EntityType.ENDER_DRAGON);
            
            if (this.dragon != null) {
                // Уменьшаем размер в 3 раза
                this.dragon.setScale(0.33);
                
                this.dragon.setPhase(EnderDragon.Phase.CIRCLING);
                this.dragon.setGravity(false);
                this.dragon.setInvulnerable(false);

                this.isSummoned = true;
                activeDragons.put(owner.getUniqueId(), this);
                lastSummonTime.put(owner.getUniqueId(), summonTime);

                startDragonTasks();
                startDespawnTimer();
                
                owner.sendMessage("§aДракон призван! Он исчезнет через 90 секунд.");
                owner.sendMessage("§eПКМ по дракону - сесть, ЛКМ по дракону - слезть");
                owner.sendMessage("§eF - зависание, ПКМ на посох - атака");
            } else {
                owner.sendMessage("§cНе удалось призвать дракона!");
            }
        } else {
            owner.sendMessage("§cМир не найден!");
        }
    }

    private void startDragonTasks() {
        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || dragon == null || dragon.isDead()) {
                    cleanup();
                    return;
                }

                // Если игрок сидит на драконе
                if (isRiding && owner.isOnline() && owner.getVehicle() != null && 
                    owner.getVehicle().equals(dragon)) {
                    
                    if (!isHovering) {
                        handleDragonMovement();
                    } else {
                        // Режим зависания - полная остановка
                        dragon.setVelocity(new Vector(0, 0, 0));
                    }
                }

                // Следование за игроком если не в седле
                if (isSummoned && !isRiding && owner.isOnline()) {
                    if (owner.getLocation().distance(dragon.getLocation()) > 10) {
                        Vector toPlayer = owner.getLocation().toVector().subtract(dragon.getLocation().toVector());
                        dragon.setVelocity(toPlayer.normalize().multiply(0.5));
                    } else {
                        dragon.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(DragonStaff.getInstance(), 0L, 1L);
    }

    private void handleDragonMovement() {
        if (dragon == null) return;
        
        Vector direction = owner.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(1.2);

        if (owner.isSneaking()) {
            velocity.setY(velocity.getY() - 0.4);
        } else if (owner.isJumping()) {
            velocity.setY(velocity.getY() + 0.4);
        }

        dragon.setVelocity(velocity);
        
        Location loc = dragon.getLocation();
        loc.setDirection(velocity);
        dragon.teleport(loc);
    }

    private void startDespawnTimer() {
        despawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isSummoned && dragon != null && !dragon.isDead()) {
                    owner.sendMessage("§cДракон исчезает... Время призыва истекло.");
                    cleanup();
                }
            }
        }.runTaskLater(DragonStaff.getInstance(), DESPAWN_TIME / 50);
    }

    public static boolean canSummon(Player player) {
        Long lastSummon = lastSummonTime.get(player.getUniqueId());
        if (lastSummon == null) return true;
        
        long timeSinceLastSummon = System.currentTimeMillis() - lastSummon;
        return timeSinceLastSummon >= SUMMON_COOLDOWN;
    }

    public static long getRemainingCooldown(Player player) {
        Long lastSummon = lastSummonTime.get(player.getUniqueId());
        if (lastSummon == null) return 0;
        
        long timeSinceLastSummon = System.currentTimeMillis() - lastSummon;
        if (timeSinceLastSummon >= SUMMON_COOLDOWN) return 0;
        
        return (SUMMON_COOLDOWN - timeSinceLastSummon) / 1000;
    }

    public void toggleHover() {
        if (!isRiding) {
            owner.sendMessage("§cВы должны сидеть на драконе, чтобы использовать режим зависания!");
            return;
        }
        
        isHovering = !isHovering;
        if (isHovering) {
            owner.sendMessage("§eРежим зависания активирован. Дракон парит на месте.");
            if (dragon != null) {
                dragon.setVelocity(new Vector(0, 0, 0));
            }
        } else {
            owner.sendMessage("§aРежим полета активирован.");
        }
    }

    public void attack() {
        if (!isSummoned || dragon == null || dragon.isDead()) return;
        
        if (!isRiding) {
            owner.sendMessage("§cВы должны сидеть на драконе, чтобы атаковать!");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < ATTACK_COOLDOWN) {
            long secondsLeft = (ATTACK_COOLDOWN - (currentTime - lastAttackTime)) / 1000;
            owner.sendMessage("§cАтака перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        lastAttackTime = currentTime;

        World world = dragon.getWorld();
        Location dragonHead = dragon.getLocation().add(0, 2.5, 0);
        Vector direction = owner.getLocation().getDirection().normalize();
        
        // Спавним вагонетку с динамитом
        TNTPrimed tnt = world.spawn(dragonHead, TNTPrimed.class);
        tnt.setFuseTicks(100); // Долгий запал, взорвётся при ударе
        tnt.setVelocity(direction.multiply(2.5));
        tnt.setYield(4.0f); // Сила взрыва
        tnt.setIsIncendiary(false); // Без огня
        tnt.setGlowing(true); // Свечение как у яйца дракона
        
        // Сохраняем снаряд для отслеживания
        this.tntProjectile = tnt;
        
        world.playSound(dragonHead, org.bukkit.Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);

        // Задача для проверки столкновения
        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt == null || tnt.isDead()) {
                    this.cancel();
                    return;
                }
                
                // Проверяем, не врезалась ли вагонетка в сущность
                for (Entity entity : tnt.getNearbyEntities(1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(owner) && !entity.equals(dragon)) {
                        tnt.remove();
                        world.createExplosion(tnt.getLocation(), 4.0f, false, false, owner);
                        world.playSound(tnt.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        this.cancel();
                        return;
                    }
                }
                
                // Если вагонетка коснулась земли или блока
                if (tnt.isOnGround()) {
                    tnt.remove();
                    world.createExplosion(tnt.getLocation(), 4.0f, false, false, owner);
                    world.playSound(tnt.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    this.cancel();
                }
                
                // Если вагонетка улетела слишком далеко (5 секунд)
                if (tnt.getTicksLived() > 100) {
                    tnt.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(DragonStaff.getInstance(), 0L, 1L);
        
        owner.sendMessage("§aДракон выстрелил динамитной вагонеткой!");
    }

    public void mountDragon() {
        if (isSummoned && dragon != null && !dragon.isDead() && !isRiding) {
            dragon.addPassenger(owner);
            this.isRiding = true;
            dragon.setGravity(false);
            owner.sendMessage("§aВы оседлали дракона!");
        }
    }

    public void dismountDragon() {
        if (isRiding && owner.getVehicle() != null && owner.getVehicle().equals(dragon)) {
            dragon.removePassenger(owner);
            this.isRiding = false;
            this.isHovering = false;
            owner.sendMessage("§eВы слезли с дракона.");
        }
    }

    private void cleanup() {
        if (dragon != null && !dragon.isDead()) {
            dragon.remove();
        }
        if (movementTask != null) {
            movementTask.cancel();
        }
        if (despawnTask != null) {
            despawnTask.cancel();
        }
        activeDragons.remove(owner.getUniqueId());
        isSummoned = false;
    }

    public EnderDragon getDragon() { return dragon; }
    public boolean isSummoned() { return isSummoned; }
    public boolean isRiding() { return isRiding; }
    public boolean isHovering() { return isHovering; }
}