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

import java.util.*;

public class RavagerHornHandler implements Listener {

    private final Map<UUID, RavagerGroup> activeGroups = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> stompCooldowns = new HashMap<>();
    
    private static final long COOLDOWN = 2 * 60 * 1000; // 2 минуты
    private static final long STOMP_COOLDOWN = 5 * 1000; // 5 секунд
    private static final int GROUP_LIFETIME = 60 * 1000; // 60 секунд
    private static final int STOMP_RADIUS = 5;
    private static final double STOMP_DAMAGE = 8.0; // 4 сердца
    private static final int FOLLOW_RADIUS = 15;

    private static class RavagerGroup {
        Ravager ravager;
        Evoker evoker;
        Illusioner illusioner;
        long spawnTime;
        UUID ownerId;
        LivingEntity target;

        RavagerGroup(Ravager ravager, Evoker evoker, Illusioner illusioner, long spawnTime, UUID ownerId) {
            this.ravager = ravager;
            this.evoker = evoker;
            this.illusioner = illusioner;
            this.spawnTime = spawnTime;
            this.ownerId = ownerId;
            this.target = null;
        }

        boolean isAlive() {
            return (ravager != null && !ravager.isDead()) ||
                   (evoker != null && !evoker.isDead()) ||
                   (illusioner != null && !illusioner.isDead());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isRavagerHorn(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            
            // Проверка кулдауна
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cРог разорителя перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Удаляем старую группу если есть
            RavagerGroup oldGroup = activeGroups.remove(player.getUniqueId());
            if (oldGroup != null) {
                if (oldGroup.ravager != null) oldGroup.ravager.remove();
                if (oldGroup.evoker != null) oldGroup.evoker.remove();
                if (oldGroup.illusioner != null) oldGroup.illusioner.remove();
            }

            Location spawnLoc = player.getLocation();
            World world = player.getWorld();

            // 1. РАЗОРИТЕЛЬ (200 HP, скорость 4)
            Ravager ravager = world.spawn(spawnLoc, Ravager.class);
            ravager.setAI(false); // Полный контроль
            ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
            ravager.setHealth(200);
            ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3)); // Скорость IV
            ravager.setTarget(null);
            ravager.setRemoveWhenFarAway(false);
            ravager.setPersistent(true);

            // 2. ЗАКЛИНАТЕЛЬ (Resistance II)
            Evoker evoker = world.spawn(spawnLoc, Evoker.class);
            evoker.setAI(false);
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            evoker.setTarget(null);
            evoker.setRemoveWhenFarAway(false);
            evoker.setPersistent(true);

            // 3. ИЛЛЮЗИОНИСТ (Resistance II)
            Illusioner illusioner = world.spawn(spawnLoc, Illusioner.class);
            illusioner.setAI(false);
            illusioner.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            illusioner.setTarget(null);
            illusioner.setRemoveWhenFarAway(false);
            illusioner.setPersistent(true);

            RavagerGroup group = new RavagerGroup(ravager, evoker, illusioner, now, player.getUniqueId());
            activeGroups.put(player.getUniqueId(), group);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
            player.sendMessage("§cРазоритель и прислужники призваны! Живут 60 секунд.");

            // Таймер исчезновения
            new BukkitRunnable() {
                @Override
                public void run() {
                    RavagerGroup current = activeGroups.get(player.getUniqueId());
                    if (current != null && current == group) {
                        if (current.ravager != null) current.ravager.remove();
                        if (current.evoker != null) current.evoker.remove();
                        if (current.illusioner != null) current.illusioner.remove();
                        activeGroups.remove(player.getUniqueId());
                        player.sendMessage("§cРазоритель и прислужники исчезли.");
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), GROUP_LIFETIME / 50);

            // Запускаем управление
            startGroupAI(player, group);

            event.setCancelled(true);
        }
    }

    private void startGroupAI(Player player, RavagerGroup group) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!group.isAlive() || System.currentTimeMillis() - group.spawnTime > GROUP_LIFETIME) {
                    this.cancel();
                    return;
                }

                // Если есть цель
                if (group.target != null && !group.target.isDead()) {
                    double distToOwner = group.target.getLocation().distance(player.getLocation());
                    
                    // Если цель слишком далеко от владельца - забываем
                    if (distToOwner > FOLLOW_RADIUS) {
                        group.target = null;
                        setGroupTarget(group, null);
                    } else {
                        setGroupTarget(group, group.target);
                    }
                }

                // Если нет цели - следуем за владельцем
                if (group.target == null || group.target.isDead()) {
                    setGroupTarget(group, player);
                }

                // Движение разорителя (для плавности)
                if (group.ravager != null && !group.ravager.isDead()) {
                    if (group.ravager.getTarget() != null) {
                        moveToward(group.ravager, group.ravager.getTarget().getLocation());
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
    }

    private void setGroupTarget(RavagerGroup group, LivingEntity target) {
        if (group.ravager != null && !group.ravager.isDead()) {
            group.ravager.setTarget(target);
        }
        if (group.evoker != null && !group.evoker.isDead()) {
            group.evoker.setTarget(target);
        }
        if (group.illusioner != null && !group.illusioner.isDead()) {
            group.illusioner.setTarget(target);
        }
    }

    private void moveToward(LivingEntity entity, Location target) {
        Vector direction = target.toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(direction.multiply(0.3));
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // Посадка на разорителя
        if (event.getRightClicked() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getRightClicked();
            RavagerGroup group = findGroupByRavager(ravager);
            
            if (group != null && group.ownerId.equals(player.getUniqueId())) {
                if (ravager.getPassengers().isEmpty()) {
                    ravager.addPassenger(player);
                    player.sendMessage("§cВы сели на разорителя! WASD - движение, ПКМ - топот");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // Спешивание
        if (event.isSneaking() && player.getVehicle() instanceof Ravager) {
            player.getVehicle().removePassenger(player);
            player.sendMessage("§cВы слезли с разорителя");
        }
    }

    @EventHandler
    public void onPlayerInteractRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Топот ПКМ верхом
        if (player.getVehicle() instanceof Ravager) {
            Ravager ravager = (Ravager) player.getVehicle();
            RavagerGroup group = findGroupByRavager(ravager);
            
            if (group != null && event.getAction() == Action.RIGHT_CLICK_AIR || 
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                long now = System.currentTimeMillis();
                Long lastStomp = stompCooldowns.get(player.getUniqueId());
                
                if (lastStomp != null && now - lastStomp < STOMP_COOLDOWN) {
                    long secondsLeft = (STOMP_COOLDOWN - (now - lastStomp)) / 1000;
                    player.sendMessage("§cТопот перезаряжается! Осталось: " + secondsLeft + " сек.");
                    event.setCancelled(true);
                    return;
                }

                // ТОПОТ
                Location stompLoc = ravager.getLocation();
                World world = stompLoc.getWorld();
                
                // Звук и эффекты
                world.playSound(stompLoc, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.5f);
                world.spawnParticle(Particle.EXPLOSION, stompLoc, 30, 2, 1, 2, 0);
                
                // Урон и замедление
                for (Entity e : world.getNearbyEntities(stompLoc, STOMP_RADIUS, STOMP_RADIUS, STOMP_RADIUS)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(ravager) &&
                        !e.equals(group.evoker) && !e.equals(group.illusioner)) {
                        
                        LivingEntity target = (LivingEntity) e;
                        target.damage(STOMP_DAMAGE, ravager);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3)); // Замедление IV на 2 сек
                        
                        // Если это не владелец и не слуги - назначаем целью
                        if (group.target == null && !target.equals(player)) {
                            group.target = target;
                            setGroupTarget(group, target);
                        }
                    }
                }
                
                stompCooldowns.put(player.getUniqueId(), now);
                player.sendMessage("§c§lТОПОТ! Урон по области + замедление");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Ravager || 
            event.getEntity() instanceof Evoker || 
            event.getEntity() instanceof Illusioner) {
            
            for (RavagerGroup group : activeGroups.values()) {
                if (group.ravager != null && group.ravager.equals(event.getEntity()) ||
                    group.evoker != null && group.evoker.equals(event.getEntity()) ||
                    group.illusioner != null && group.illusioner.equals(event.getEntity())) {
                    
                    // Не атакуют владельца
                    if (event.getTarget() instanceof Player && 
                        ((Player) event.getTarget()).getUniqueId().equals(group.ownerId)) {
                        event.setCancelled(true);
                        return;
                    }
                    
                    // Атакуют только заданную цель
                    if (group.target != null && !event.getTarget().equals(group.target)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private RavagerGroup findGroupByRavager(Ravager ravager) {
        for (RavagerGroup group : activeGroups.values()) {
            if (group.ravager != null && group.ravager.equals(ravager)) {
                return group;
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