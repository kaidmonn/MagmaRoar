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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class StormBladeHandler implements Listener {

    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final Map<UUID, Long> passiveCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isActive = new HashMap<>();
    private final Random random = new Random();
    
    private static final long ABILITY_COOLDOWN = 30 * 1000; // 30 секунд
    private static final long PASSIVE_COOLDOWN = 100; // Минимальная задержка между пассивками
    private static final double PASSIVE_CHANCE = 0.15; // 15% шанс
    private static final double WEAPON_DAMAGE = 14.0;
    private static final float EXPLOSION_POWER = 4.0f;
    private static final int LAUNCH_HEIGHT = 8;
    private static final double PROJECTILE_SPREAD = 0.1; // Минимальный разброс

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isStormBlade(item)) return;

        // Устанавливаем урон 14
        event.setDamage(WEAPON_DAMAGE);

        // Проверяем, не в активном режиме ли (чтобы не спамить пассивки)
        if (isActive.getOrDefault(player.getUniqueId(), false)) return;

        // Проверяем пассивный шанс
        long now = System.currentTimeMillis();
        Long lastPassive = passiveCooldowns.get(player.getUniqueId());
        
        if (random.nextDouble() < PASSIVE_CHANCE && 
            (lastPassive == null || now - lastPassive > PASSIVE_COOLDOWN)) {
            
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                activatePassive(player, target);
                passiveCooldowns.put(player.getUniqueId(), now);
            }
        }
    }

    private void activatePassive(Player player, LivingEntity target) {
        World world = target.getWorld();
        Location targetLoc = target.getLocation();
        
        // Подбрасываем на 8 блоков
        Vector velocity = target.getVelocity();
        velocity.setY(LAUNCH_HEIGHT * 0.4);
        target.setVelocity(velocity);
        
        player.sendMessage("§b§lШТОРМ! Цель подброшена!");
        
        // Запускаем таймер на молнию в момент падения
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (target == null || target.isDead() || target.isOnGround() || ticks > 60) {
                    
                    if (target != null && !target.isDead()) {
                        // Молния в момент падения
                        Location strikeLoc = target.getLocation();
                        world.strikeLightningEffect(strikeLoc);
                        world.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                        
                        // Урон от молнии (2.5 сердца)
                        target.damage(5.0, player);
                        
                        // Визуальные эффекты
                        world.spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                        
                        player.sendMessage("§bМолния поражает цель!");
                    }
                    
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 20L, 1L); // Проверяем каждую секунду
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isStormBlade(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Проверяем, не активна ли уже способность
            if (isActive.getOrDefault(player.getUniqueId(), false)) {
                player.sendMessage("§cСпособность уже активна!");
                event.setCancelled(true);
                return;
            }
            
            long now = System.currentTimeMillis();
            Long lastUse = abilityCooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < ABILITY_COOLDOWN) {
                long secondsLeft = (ABILITY_COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cСпособность перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Получаем направление взгляда
            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            World world = player.getWorld();

            player.sendMessage("§b§lКЛИНОК БУРИ! 10 молний! (кулдаун 30 сек)");
            isActive.put(player.getUniqueId(), true);
            
            // Звук начала
            world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);

            // Запускаем 10 молний-снарядов
            for (int i = 0; i < 10; i++) {
                // Небольшой разброс для каждой молнии (0.1 блока)
                double spreadX = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                double spreadY = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                double spreadZ = (Math.random() - 0.5) * PROJECTILE_SPREAD * 2;
                
                Vector shotDirection = direction.clone().add(new Vector(spreadX, spreadY, spreadZ)).normalize();
                
                // Создаём снежок как снаряд
                Snowball projectile = world.spawn(eyeLoc, Snowball.class);
                projectile.setVelocity(shotDirection.multiply(2.5));
                projectile.setGlowing(true);
                projectile.setShooter(player);
                
                // Сохраняем, что это наш снаряд (можно через метадату)
                projectile.setCustomName("§bМолния");
            }
            
            abilityCooldowns.put(player.getUniqueId(), now);
            
            // Через 1 секунду отключаем активный режим
            new BukkitRunnable() {
                @Override
                public void run() {
                    isActive.remove(player.getUniqueId());
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), 20L);
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        if (!(snowball.getShooter() instanceof Player)) return;
        
        Player player = (Player) snowball.getShooter();
        
        // Проверяем, что это наш снаряд
        if (snowball.getCustomName() == null || !snowball.getCustomName().contains("Молния")) return;
        
        Location hitLoc = snowball.getLocation();
        World world = hitLoc.getWorld();
        
        // Визуал молнии
        world.strikeLightningEffect(hitLoc);
        
        // ВЗРЫВ УРОВНЯ 4 (без разрушения блоков)
        world.createExplosion(hitLoc, EXPLOSION_POWER, false, true, player);
        
        // Визуальные эффекты
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 30, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.FLASH, hitLoc, 5, 1, 1, 1, 0);
        
        // Звук
        world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
        
        // Убираем снежок
        snowball.remove();
    }

    private boolean isStormBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Клинок бури");
    }
}