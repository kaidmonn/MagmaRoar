package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RavagerHornHandler implements Listener {

    private final Map<UUID, RavagerInfo> activeRavagers = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> stompCooldowns = new HashMap<>();
    
    private static final long COOLDOWN = 2 * 60 * 1000;
    private static final long STOMP_COOLDOWN = 5 * 1000;
    private static final int RAVAGER_LIFETIME = 60 * 1000;
    private static final int STOMP_RADIUS = 5;
    private static final double STOMP_DAMAGE = 8.0;

    private static class RavagerInfo {
        Ravager ravager;
        long spawnTime;
        UUID ownerId;

        RavagerInfo(Ravager ravager, long spawnTime, UUID ownerId) {
            this.ravager = ravager;
            this.spawnTime = spawnTime;
            this.ownerId = ownerId;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isRavagerHorn(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cРог разорителя перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            RavagerInfo oldRavager = activeRavagers.remove(player.getUniqueId());
            if (oldRavager != null && oldRavager.ravager != null && !oldRavager.ravager.isDead()) {
                oldRavager.ravager.remove();
            }

            Location spawnLoc = player.getLocation();
            World world = player.getWorld();

            Ravager ravager = world.spawn(spawnLoc, Ravager.class);
            
            // Полностью отключаем ИИ
            ravager.setAI(false);
            ravager.setTarget(null);
            
            // Устанавливаем атрибуты
            ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
            ravager.setHealth(200);
            ravager.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
            
            // Добавляем эффекты
            ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
            ravager.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            
            ravager.setRemoveWhenFarAway(false);
            ravager.setPersistent(true);

            RavagerInfo info = new RavagerInfo(ravager, now, player.getUniqueId());
            activeRavagers.put(player.getUniqueId(), info);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
            player.sendMessage("§cРазоритель призван! Живёт 60 секунд.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    RavagerInfo current = activeRavagers.get(player.getUniqueId());
                    if (current != null && current == info) {
                        if (current.ravager != null && !current.ravager.isDead()) {
                            current.ravager.remove();
                        }
                        activeRavagers.remove(player.getUniqueId());
                        player.sendMessage("§cРазоритель исчез.");
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), RAVAGER_LIFETIME / 50);

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (event.getRightClicked() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getRightClicked();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null && info.ownerId.equals(player.getUniqueId())) {
                if (ravager.getPassengers().isEmpty()) {
                    ravager.addPassenger(player);
                    player.sendMessage("§cВы сели на разорителя! WASD - движение, Пробел - прыжок, ПКМ - топот");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (event.isSneaking() && player.getVehicle() instanceof Ravager) {
            player.getVehicle().removePassenger(player);
            player.sendMessage("§cВы слезли с разорителя");
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (player.getVehicle() instanceof Ravager) {
            Ravager ravager = (Ravager) player.getVehicle();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null) {
                // Получаем направление взгляда игрока
                Vector direction = player.getLocation().getDirection();
                
                // Убираем вертикальную составляющую
                direction.setY(0);
                
                // Если игрок нажимает W (движение вперед)
                if (direction.lengthSquared() > 0) {
                    direction.normalize();
                    
                    // Двигаем разорителя
                    Vector velocity = direction.multiply(0.8);
                    ravager.setVelocity(velocity);
                    
                    // Поворачиваем разорителя в сторону движения
                    Location loc = ravager.getLocation();
                    loc.setYaw(player.getLocation().getYaw());
                    ravager.teleport(loc);
                } else {
                    // Если игрок не двигается, останавливаем разорителя
                    ravager.setVelocity(new Vector(0, ravager.getVelocity().getY(), 0));
                }
                
                // Прыжок (пробел)
                if (player.isJumping() && ravager.isOnGround()) {
                    Vector jump = ravager.getVelocity();
                    jump.setY(0.6);
                    ravager.setVelocity(jump);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (player.getVehicle() instanceof Ravager) {
            Ravager ravager = (Ravager) player.getVehicle();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null && (event.getAction() == Action.RIGHT_CLICK_AIR || 
                event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                
                long now = System.currentTimeMillis();
                Long lastStomp = stompCooldowns.get(player.getUniqueId());
                
                if (lastStomp != null && now - lastStomp < STOMP_COOLDOWN) {
                    long secondsLeft = (STOMP_COOLDOWN - (now - lastStomp)) / 1000;
                    player.sendMessage("§cТопот перезаряжается! Осталось: " + secondsLeft + " сек.");
                    event.setCancelled(true);
                    return;
                }

                Location stompLoc = ravager.getLocation();
                World world = stompLoc.getWorld();
                
                world.playSound(stompLoc, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.5f);
                world.spawnParticle(Particle.EXPLOSION, stompLoc, 30, 2, 1, 2, 0);
                
                for (Entity e : world.getNearbyEntities(stompLoc, STOMP_RADIUS, STOMP_RADIUS, STOMP_RADIUS)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(ravager)) {
                        LivingEntity target = (LivingEntity) e;
                        target.damage(STOMP_DAMAGE, ravager);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
                    }
                }
                
                stompCooldowns.put(player.getUniqueId(), now);
                player.sendMessage("§c§lТОПОТ!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getEntity();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null) {
                // Полностью отключаем любое таргетирование
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getEntity();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null) {
                // Отключаем урон от падения
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getDamager();
            RavagerInfo info = findRavager(ravager);
            
            if (info != null) {
                // Разоритель не может никого атаковать (кроме топата)
                event.setCancelled(true);
            }
        }
    }

    private RavagerInfo findRavager(Ravager ravager) {
        for (RavagerInfo info : activeRavagers.values()) {
            if (info.ravager != null && info.ravager.equals(ravager)) {
                return info;
            }
        }
        return null;
    }

    private boolean isRavagerHorn(ItemStack item) {
        if (item == null || item.getType() != Material.GOAT_HORN || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Рог разорителя");
    }
}