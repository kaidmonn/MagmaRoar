package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
    private final Strider strider;
    private boolean isSummoned = false;
    private boolean isRiding = false;
    private long lastAttackTime = 0;
    private long lastJumpTime = 0;
    private long summonTime = 0;
    private static final long ATTACK_COOLDOWN = 20 * 1000;
    private static final long JUMP_COOLDOWN = 2000;
    private static final long DESPAWN_TIME = 90 * 1000;
    private static final long SUMMON_COOLDOWN = 3 * 60 * 1000;
    private BukkitTask despawnTask;
    private BukkitTask movementTask;
    private BukkitTask fireTrailTask;

    public static final Map<UUID, MagmaRoar> activeMagmaRoars = new HashMap<>();
    public static final Map<UUID, Long> lastSummonTime = new HashMap<>();

    public MagmaRoar(Player owner, Location location) {
        this.owner = owner;
        this.summonTime = System.currentTimeMillis();

        World world = location.getWorld();
        if (world != null) {
            this.strider = (Strider) world.spawnEntity(location, EntityType.STRIDER);
            
            if (this.strider != null) {
                this.strider.setScale(2.0);
                this.strider.setHealth(48);
                this.strider.setSaddle(true);
                this.strider.setInvulnerable(false);
                
                this.strider.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                this.strider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false));

                this.isSummoned = true;
                activeMagmaRoars.put(owner.getUniqueId(), this);
                lastSummonTime.put(owner.getUniqueId(), summonTime);

                world.playSound(location, org.bukkit.Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);

                startTasks();
                startDespawnTimer();
                
                owner.sendMessage("§aМагма Рёв призван! Он исчезнет через 90 секунд.");
            }
        }
    }

    private void startTasks() {
        fireTrailTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    cleanup();
                    return;
                }

                if (strider.isOnGround()) {
                    Location footLoc = strider.getLocation().subtract(0, 1, 0);
                    if (footLoc.getBlock().getType() == Material.AIR || 
                        footLoc.getBlock().getType() == Material.FIRE) {
                        footLoc.getBlock().setType(Material.FIRE);
                    }
                }

                if (!strider.getPassengers().isEmpty() && strider.getPassengers().get(0) instanceof Player) {
                    Player rider = (Player) strider.getPassengers().get(0);
                    
                    if (!rider.equals(owner)) {
                        strider.removePassenger(rider);
                        rider.sendMessage("§cВы не можете сесть на чужого Магма Рёва!");
                    } else {
                        rider.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                        isRiding = true;
                    }
                } else {
                    isRiding = false;
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);

        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSummoned || strider == null || strider.isDead()) {
                    cleanup();
                    return;
                }

                if (isRiding && owner.isOnline() && owner.getVehicle() != null && 
                    owner.getVehicle().equals(strider)) {
                    
                    Vector direction = owner.getLocation().getDirection().normalize();
                    Vector velocity = direction.multiply(0.5);
                    velocity.setY(strider.getVelocity().getY());
                    strider.setVelocity(velocity);
                }

                if (isSummoned && !isRiding && owner.isOnline()) {
                    if (owner.getLocation().distance(strider.getLocation()) > 10) {
                        Vector toPlayer = owner.getLocation().toVector().subtract(strider.getLocation().toVector());
                        strider.setVelocity(toPlayer.normalize().multiply(0.5));
                    } else {
                        strider.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    public void jump() {
        if (strider == null || strider.isDead()) return;
        if (!isRiding) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastJumpTime < JUMP_COOLDOWN) return;

        lastJumpTime = currentTime;

        strider.setVelocity(strider.getVelocity().add(0, 0.6, 0));
        strider.getWorld().playSound(strider.getLocation(), org.bukkit.Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
        
        Location jumpLoc = strider.getLocation().subtract(0, 1, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location fireLoc = jumpLoc.clone().add(x, 0, z);
                if (fireLoc.getBlock().getType() == Material.AIR || 
                    fireLoc.getBlock().getType() == Material.FIRE) {
                    fireLoc.getBlock().setType(Material.FIRE);
                }
            }
        }
        
        strider.getWorld().spawnParticle(org.bukkit.Particle.FLAME, strider.getLocation(), 30, 1, 0.5, 1, 0.1);
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
        Location roarHead = strider.getLocation().add(0, 1.5, 0);
        Vector direction = owner.getLocation().getDirection().normalize();
        
        TNTPrimed tnt = world.spawn(roarHead, TNTPrimed.class);
        tnt.setFuseTicks(100);
        tnt.setVelocity(direction.multiply(2.0));
        tnt.setYield(4.0f);
        tnt.setIsIncendiary(false);
        tnt.setGlowing(true);
        
        world.playSound(roarHead, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt == null || tnt.isDead()) {
                    this.cancel();
                    return;
                }
                
                if (tnt.isOnGround() || tnt.getTicksLived() > 100) {
                    world.createExplosion(tnt.getLocation(), 4.0f, false, false, owner);
                    world.playSound(tnt.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    tnt.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        
        owner.sendMessage("§aМагма Рёв выстрелил TNT!");
    }

    public void mount(Player player) {
        if (!player.equals(owner)) {
            player.sendMessage("§cЭто не ваш Магма Рёв!");
            return;
        }
        
        if (isSummoned && strider != null && !strider.isDead() && !isRiding) {
            strider.addPassenger(owner);
            isRiding = true;
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