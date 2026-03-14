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
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HypnosisStaffHandler implements Listener {

    private final Map<UUID, WardenInfo> activeWardens = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 90 * 1000;
    private static final int WARDEN_LIFETIME = 40 * 1000;
    private static final int FOLLOW_RADIUS = 10;

    private static class WardenInfo {
        Warden warden;
        long spawnTime;
        LivingEntity target;
        UUID ownerId;

        WardenInfo(Warden warden, long spawnTime, UUID ownerId) {
            this.warden = warden;
            this.spawnTime = spawnTime;
            this.ownerId = ownerId;
            this.target = null;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isHypnosisStaff(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            long now = System.currentTimeMillis();

            WardenInfo info = activeWardens.get(player.getUniqueId());

            if (info != null && info.warden != null && !info.warden.isDead()) {
                info.warden.teleport(player.getLocation());
                player.sendMessage("§5Варден телепортирован к вам!");
                event.setCancelled(true);
                return;
            }

            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЖезл гипноза перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            Location spawnLoc = player.getLocation();
            World world = player.getWorld();

            Warden warden = world.spawn(spawnLoc, Warden.class);

            // Полный контроль над Варденом
            warden.setAI(false); // Отключаем встроенный ИИ
            warden.setTarget(null);
            warden.setHealth(100);
            warden.setRemoveWhenFarAway(false);
            warden.setPersistent(true);

            // Добавляем эффекты для скорости и живучести
            warden.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
            warden.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));

            WardenInfo newInfo = new WardenInfo(warden, now, player.getUniqueId());
            activeWardens.put(player.getUniqueId(), newInfo);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
            player.sendMessage("§5Варден призван! Он будет следовать за вами 40 секунд.");

            // Таймер исчезновения
            new BukkitRunnable() {
                @Override
                public void run() {
                    WardenInfo current = activeWardens.get(player.getUniqueId());
                    if (current != null && current.warden != null && !current.warden.isDead()) {
                        current.warden.remove();
                        activeWardens.remove(player.getUniqueId());
                        player.sendMessage("§5Варден исчез...");
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), WARDEN_LIFETIME / 50);

            // Запускаем кастомное управление
            startCustomAI(player, newInfo);

            event.setCancelled(true);
        }
    }

    private void startCustomAI(Player player, WardenInfo info) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (info.warden == null || info.warden.isDead() ||
                    System.currentTimeMillis() - info.spawnTime > WARDEN_LIFETIME) {
                    this.cancel();
                    return;
                }

                // Всегда отменяем любые попытки вардена атаковать владельца
                if (info.warden.getTarget() != null && info.warden.getTarget().equals(player)) {
                    info.warden.setTarget(null);
                }

                // Если есть цель
                if (info.target != null && !info.target.isDead()) {
                    double distToTarget = info.warden.getLocation().distance(info.target.getLocation());
                    double distToOwner = info.warden.getLocation().distance(player.getLocation());

                    // Если цель дальше FOLLOW_RADIUS от владельца - забываем
                    if (info.target.getLocation().distance(player.getLocation()) > FOLLOW_RADIUS) {
                        info.target = null;
                    } else {
                        // Идём к цели
                        moveToward(info.warden, info.target.getLocation());

                        // Если рядом - атакуем
                        if (distToTarget < 2) {
                            info.warden.attack(info.target);
                        }
                    }
                }

                // Если нет цели - следуем за владельцем
                if (info.target == null || info.target.isDead()) {
                    double distToOwner = info.warden.getLocation().distance(player.getLocation());

                    if (distToOwner > 3) {
                        moveToward(info.warden, player.getLocation());
                    } else {
                        // Стоим на месте
                        info.warden.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 2L); // Каждые 2 тика для плавности
    }

    private void moveToward(LivingEntity entity, Location target) {
        org.bukkit.util.Vector direction = target.toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(direction.multiply(0.3));
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        // Блокируем Вардена от самостоятельного выбора цели
        if (event.getEntity() instanceof Warden) {
            Warden warden = (Warden) event.getEntity();

            // Проверяем, принадлежит ли этот Варден кому-то
            for (WardenInfo info : activeWardens.values()) {
                if (info.warden != null && info.warden.equals(warden)) {
                    // Если цель - владелец, отменяем
                    if (event.getTarget() instanceof Player &&
                        ((Player) event.getTarget()).getUniqueId().equals(info.ownerId)) {
                        event.setCancelled(true);
                    }
                    // Если цели нет или это кто-то другой - разрешаем (но наш AI перехватит)
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isHypnosisStaff(item)) return;

        WardenInfo info = activeWardens.get(player.getUniqueId());
        if (info != null && info.warden != null && !info.warden.isDead()) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();

                // Нельзя атаковать владельца или других варденов
                if (target.equals(player) || target instanceof Warden) {
                    return;
                }

                info.target = target;
                player.sendMessage("§5Варден атакует: " + (target instanceof Player ? target.getName() : "моб"));

                target.getWorld().spawnParticle(Particle.SCULK_SOUL,
                    target.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.1);
            }
        }
    }

    private boolean isHypnosisStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Жезл гипноза");
    }
}