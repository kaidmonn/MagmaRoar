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
    
    // Карты для хранения состояния клавиш WASD
    private final Map<UUID, Boolean> wPressed = new HashMap<>();
    private final Map<UUID, Boolean> aPressed = new HashMap<>();
    private final Map<UUID, Boolean> sPressed = new HashMap<>();
    private final Map<UUID, Boolean> dPressed = new HashMap<>();
    
    private static final long COOLDOWN = 2 * 60 * 1000;
    private static final long STOMP_COOLDOWN = 5 * 1000;
    private static final int GROUP_LIFETIME = 60 * 1000;
    private static final int STOMP_RADIUS = 5;
    private static final double STOMP_DAMAGE = 8.0;
    private static final int FOLLOW_RADIUS = 15;
    private static final double MOVE_SPEED = 0.8;

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

            // 1. РАЗОРИТЕЛЬ
            Ravager ravager = world.spawn(spawnLoc, Ravager.class);
            ravager.setAI(false);
            ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
            ravager.setHealth(200);
            ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
            ravager.setTarget(null);
            ravager.setRemoveWhenFarAway(false);
            ravager.setPersistent(true);

            // 2. ЗАКЛИНАТЕЛЬ
            Evoker evoker = world.spawn(spawnLoc, Evoker.class);
            evoker.setAI(false);
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            evoker.setTarget(null);
            evoker.setRemoveWhenFarAway(false);
            evoker.setPersistent(true);

            // 3. ИЛЛЮЗИОНИСТ
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
                    
                    // Инициализируем WASD карты для игрока
                    wPressed.put(player.getUniqueId(), false);
                    aPressed.put(player.getUniqueId(), false);
                    sPressed.put(player.getUniqueId(), false);
                    dPressed.put(player.getUniqueId(), false);
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
            
            // Очищаем WASD карты при спешивании
            wPressed.remove(player.getUniqueId());
            aPressed.remove(player.getUniqueId());
            sPressed.remove(player.getUniqueId());
            dPressed.remove(player.getUniqueId());
        }
    }

    // Обработка нажатий клавиш WASD
    @EventHandler
    public void onPlayerInteractRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!(player.getVehicle() instanceof Ravager)) return;
        
        // Это для топота (ПКМ)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleStomp(player);
            event.setCancelled(true);
        }
    }

    private void handleStomp(Player player) {
        if (!(player.getVehicle() instanceof Ravager)) return;
        
        Ravager ravager = (Ravager) player.getVehicle();
        RavagerGroup group = findGroupByRavager(ravager);
        
        if (group == null) return;
        
        long now = System.currentTimeMillis();
        Long lastStomp = stompCooldowns.get(player.getUniqueId());
        
        if (lastStomp != null && now - lastStomp < STOMP_COOLDOWN) {
            long secondsLeft = (STOMP_COOLDOWN - (now - lastStomp)) / 1000;
            player.sendMessage("§cТопот перезаряжается! Осталось: " + secondsLeft + " сек.");
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
                }
            }
        }
        
        stompCooldowns.put(player.getUniqueId(), now);
        player.sendMessage("§c§lТОПОТ!");
    }

    private void startGroupAI(Player player, RavagerGroup group) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!group.isAlive() || System.currentTimeMillis() - group.spawnTime > GROUP_LIFETIME) {
                    this.cancel();
                    return;
                }

                // Полная защита от атаки владельца
                protectFromOwner(player, group);

                // ДВИЖЕНИЕ РАЗОРИТЕЛЯ ПРИ ЕЗДЕ
                if (group.ravager != null && !group.ravager.isDead() && !group.ravager.getPassengers().isEmpty()) {
                    Player rider = (Player) group.ravager.getPassengers().get(0);
                    if (rider.equals(player)) {
                        moveRavagerWithWASD(rider, group.ravager);
                    }
                } else {
                    // Если на разорителе никто не сидит - он следует за владельцем
                    followOwner(player, group);
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void moveRavagerWithWASD(Player player, Ravager ravager) {
        // Получаем направление взгляда игрока для определения "вперед"
        Vector forward = player.getLocation().getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        
        Vector movement = new Vector(0, 0, 0);
        
        // Собираем движение на основе нажатых клавиш
        if (wPressed.getOrDefault(player.getUniqueId(), false)) {
            movement.add(forward.clone().multiply(MOVE_SPEED));
        }
        if (sPressed.getOrDefault(player.getUniqueId(), false)) {
            movement.subtract(forward.clone().multiply(MOVE_SPEED));
        }
        if (aPressed.getOrDefault(player.getUniqueId(), false)) {
            movement.subtract(right.clone().multiply(MOVE_SPEED));
        }
        if (dPressed.getOrDefault(player.getUniqueId(), false)) {
            movement.add(right.clone().multiply(MOVE_SPEED));
        }
        
        // Применяем движение
        if (movement.lengthSquared() > 0) {
            ravager.setVelocity(movement);
            
            // Поворачиваем разорителя в сторону движения
            Location loc = ravager.getLocation();
            loc.setDirection(movement);
            ravager.teleport(loc);
        }
    }

    private void protectFromOwner(Player player, RavagerGroup group) {
        if (group.ravager != null && !group.ravager.isDead() && 
            group.ravager.getTarget() != null && group.ravager.getTarget().equals(player)) {
            group.ravager.setTarget(null);
        }
        if (group.evoker != null && !group.evoker.isDead() && 
            group.evoker.getTarget() != null && group.evoker.getTarget().equals(player)) {
            group.evoker.setTarget(null);
        }
        if (group.illusioner != null && !group.illusioner.isDead() && 
            group.illusioner.getTarget() != null && group.illusioner.getTarget().equals(player)) {
            group.illusioner.setTarget(null);
        }
    }

    private void followOwner(Player player, RavagerGroup group) {
        if (group.ravager != null && !group.ravager.isDead()) {
            if (group.ravager.getLocation().distance(player.getLocation()) > 3) {
                Vector toPlayer = player.getLocation().toVector().subtract(group.ravager.getLocation().toVector()).normalize();
                group.ravager.setVelocity(toPlayer.multiply(0.3));
            }
        }
    }

    // Методы для отслеживания WASD (добавь их в класс)
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Ravager)) return;
        
        // Проверяем нажатые клавиши через разницу в скорости
        Vector vel = player.getVelocity();
        wPressed.put(player.getUniqueId(), vel.getZ() > 0.1);
        sPressed.put(player.getUniqueId(), vel.getZ() < -0.1);
        aPressed.put(player.getUniqueId(), vel.getX() < -0.1);
        dPressed.put(player.getUniqueId(), vel.getX() > 0.1);
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