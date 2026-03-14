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
import org.bukkit.event.entity.EntityDamageEvent;
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

    private final Map<UUID, HorseInfo> activeHorses = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> stompCooldowns = new HashMap<>();
    
    private static final long COOLDOWN = 2 * 60 * 1000;
    private static final long STOMP_COOLDOWN = 5 * 1000;
    private static final int HORSE_LIFETIME = 60 * 1000;
    private static final int STOMP_RADIUS = 5;
    private static final double STOMP_DAMAGE = 8.0;

    private static class HorseInfo {
        Horse horse;
        long spawnTime;
        UUID ownerId;

        HorseInfo(Horse horse, long spawnTime, UUID ownerId) {
            this.horse = horse;
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

            HorseInfo oldHorse = activeHorses.remove(player.getUniqueId());
            if (oldHorse != null && oldHorse.horse != null && !oldHorse.horse.isDead()) {
                oldHorse.horse.remove();
            }

            Location spawnLoc = player.getLocation();
            World world = player.getWorld();

            // СОЗДАЁМ КОНЯ
            Horse horse = world.spawn(spawnLoc, Horse.class);
            
            // Настраиваем под разорителя
            horse.setAdult();
            horse.setTamed(true);
            horse.setOwner(player);
            horse.setDomestication(100);
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            
            // Делаем похожим на разорителя
            horse.setColor(Horse.Color.DARK_BROWN);
            horse.setStyle(Horse.Style.NONE);
            
            // Характеристики разорителя
            horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
            horse.setHealth(200);
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
            horse.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(1.0);
            
            // Эффекты
            horse.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
            horse.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));

            HorseInfo info = new HorseInfo(horse, now, player.getUniqueId());
            activeHorses.put(player.getUniqueId(), info);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.0f);
            player.sendMessage("§cБоевой конь призван! Живёт 60 секунд. ПКМ по коню - сесть.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    HorseInfo current = activeHorses.get(player.getUniqueId());
                    if (current != null && current == info) {
                        if (current.horse != null && !current.horse.isDead()) {
                            current.horse.remove();
                        }
                        activeHorses.remove(player.getUniqueId());
                        player.sendMessage("§cБоевой конь исчез.");
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), HORSE_LIFETIME / 50);

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (event.getRightClicked() instanceof Horse) {
            Horse horse = (Horse) event.getRightClicked();
            HorseInfo info = findHorse(horse);
            
            if (info != null && info.ownerId.equals(player.getUniqueId())) {
                if (horse.getPassengers().isEmpty()) {
                    horse.addPassenger(player);
                    player.sendMessage("§cВы сели на коня! WASD - движение, Пробел - прыжок, ПКМ - топот");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (event.isSneaking() && player.getVehicle() instanceof Horse) {
            player.getVehicle().removePassenger(player);
            player.sendMessage("§cВы слезли с коня");
        }
    }

    @EventHandler
    public void onPlayerInteractRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (player.getVehicle() instanceof Horse) {
            Horse horse = (Horse) player.getVehicle();
            HorseInfo info = findHorse(horse);
            
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

                Location stompLoc = horse.getLocation();
                World world = stompLoc.getWorld();
                
                world.playSound(stompLoc, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.5f);
                world.spawnParticle(Particle.EXPLOSION, stompLoc, 30, 2, 1, 2, 0);
                
                for (Entity e : world.getNearbyEntities(stompLoc, STOMP_RADIUS, STOMP_RADIUS, STOMP_RADIUS)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(horse)) {
                        LivingEntity target = (LivingEntity) e;
                        target.damage(STOMP_DAMAGE, horse);
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
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Horse) {
            Horse horse = (Horse) event.getEntity();
            HorseInfo info = findHorse(horse);
            if (info != null) {
                event.setCancelled(true);
            }
        }
    }

    private HorseInfo findHorse(Horse horse) {
        for (HorseInfo info : activeHorses.values()) {
            if (info.horse != null && info.horse.equals(horse)) {
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