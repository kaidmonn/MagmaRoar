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
    
    private static final long COOLDOWN = 2 * 60 * 1000;
    private static final long STOMP_COOLDOWN = 5 * 1000;
    private static final int GROUP_LIFETIME = 60 * 1000;
    private static final int STOMP_RADIUS = 5;
    private static final double STOMP_DAMAGE = 8.0;
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
            
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cРог разорителя перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

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
            ravager.setAI(true);
            ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
            ravager.setHealth(200);
            ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
            ravager.setTarget(null);
            ravager.setRemoveWhenFarAway(false);
            ravager.setPersistent(true);

            // 2. ЗАКЛИНАТЕЛЬ (Resistance II)
            Evoker evoker = world.spawn(spawnLoc, Evoker.class);
            evoker.setAI(true);
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            evoker.setTarget(null);
            evoker.setRemoveWhenFarAway(false);
            evoker.setPersistent(true);

            // 3. ИЛЛЮЗИОНИСТ (Resistance II)
            Illusioner illusioner = world.spawn(spawnLoc, Illusioner.class);
            illusioner.setAI(true);
            illusioner.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            illusioner.setTarget(null);
            illusioner.setRemoveWhenFarAway(false);
            illusioner.setPersistent(true);

            RavagerGroup group = new RavagerGroup(ravager, evoker, illusioner, now, player.getUniqueId());
            activeGroups.put(player.getUniqueId(), group);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
            player.sendMessage("§cРазоритель и прислужники призваны! Живут 60 секунд.");

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

                // ЗАЩИТА ОТ АТАКИ ВЛАДЕЛЬЦА
                if (group.ravager != null && !group.ravager.isDead()) {
                    if (group.ravager.getTarget() != null && group.ravager.getTarget().equals(player)) {
                        group.ravager.setTarget(null);
                    }
                }
                if (group.evoker != null && !group.evoker.isDead()) {
                    if (group.evoker.getTarget() != null && group.evoker.getTarget().equals(player)) {
                        group.evoker.setTarget(null);
                    }
                }
                if (group.illusioner != null && !group.illusioner.isDead()) {
                    if (group.illusioner.getTarget() != null && group.illusioner.getTarget().equals(player)) {
                        group.illusioner.setTarget(null);
                    }
                }

                // Если есть цель
                if (group.target != null && !group.target.isDead()) {
                    double distToOwner = group.target.getLocation().distance(player.getLocation());
                    
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

                // УПРАВЛЕНИЕ РАЗОРИТЕЛЕМ ПРИ ЕЗДЕ
                if (group.ravager != null && !group.ravager.isDead() && !group.ravager.getPassengers().isEmpty()) {
                    Player rider = (Player) group.ravager.getPassengers().get(0);
                    if (rider.equals(player)) {
                        // Движение по направлению взгляда
                        Vector direction = rider.getLocation().getDirection().normalize();
                        Vector velocity = new Vector(direction.getX() * 0.5, 0, direction.getZ() * 0.5);
                        group.ravager.setVelocity(velocity);
                        
                        // ПОВОРОТ ВСЕГО ТЕЛА
                        Location loc = group.ravager.getLocation();
                        loc.setYaw(rider.getLocation().getYaw());
                        loc.setPitch(0);
                        group.ravager.teleport(loc);
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 2L);
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

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
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
        
        if (event.isSneaking() && player.getVehicle() instanceof Ravager) {
            player.getVehicle().removePassenger(player);
            player.sendMessage("§cВы слезли с разорителя");
        }
    }

    @EventHandler
    public void onPlayerInteractRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (player.getVehicle() instanceof Ravager) {
            Ravager ravager = (Ravager) player.getVehicle();
            RavagerGroup group = findGroupByRavager(ravager);
            
            if (group != null && (event.getAction() == Action.RIGHT_CLICK_AIR || 
                event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                
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
                
                world.playSound(stompLoc, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.5f);
                world.spawnParticle(Particle.EXPLOSION, stompLoc, 30, 2, 1, 2, 0);
                
                for (Entity e : world.getNearbyEntities(stompLoc, STOMP_RADIUS, STOMP_RADIUS, STOMP_RADIUS)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(ravager) &&
                        !e.equals(group.evoker) && !e.equals(group.illusioner)) {
                        
                        LivingEntity target = (LivingEntity) e;
                        target.damage(STOMP_DAMAGE, ravager);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
                        
                        if (group.target == null && !target.equals(player)) {
                            group.target = target;
                            setGroupTarget(group, target);
                        }
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
        if (event.getEntity() instanceof Ravager || 
            event.getEntity() instanceof Evoker || 
            event.getEntity() instanceof Illusioner) {
            
            for (RavagerGroup group : activeGroups.values()) {
                if (group.ravager != null && group.ravager.equals(event.getEntity()) ||
                    group.evoker != null && group.evoker.equals(event.getEntity()) ||
                    group.illusioner != null && group.illusioner.equals(event.getEntity())) {
                    
                    if (event.getTarget() instanceof Player && 
                        ((Player) event.getTarget()).getUniqueId().equals(group.ownerId)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Защита от урона по владельцу
        if (event.getDamager() instanceof Ravager || 
            event.getDamager() instanceof Evoker || 
            event.getDamager() instanceof Illusioner) {
            
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                
                for (RavagerGroup group : activeGroups.values()) {
                    if (group.ownerId.equals(player.getUniqueId())) {
                        if ((group.ravager != null && group.ravager.equals(event.getDamager())) ||
                            (group.evoker != null && group.evoker.equals(event.getDamager())) ||
                            (group.illusioner != null && group.illusioner.equals(event.getDamager()))) {
                            event.setCancelled(true);
                            return;
                        }
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