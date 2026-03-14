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
    private static final int ANGER_AMOUNT = 150;

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

            // Настройка Вардена
            warden.setAI(true);
            warden.setTarget(null);
            warden.setHealth(100);
            warden.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));

            WardenInfo newInfo = new WardenInfo(warden, now, player.getUniqueId());
            activeWardens.put(player.getUniqueId(), newInfo);
            cooldowns.put(player.getUniqueId(), now);

            world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
            player.sendMessage("§5Варден призван!");

            // Таймер исчезновения
            new BukkitRunnable() {
                @Override
                public void run() {
                    WardenInfo current = activeWardens.get(player.getUniqueId());
                    if (current != null && current.warden != null && !current.warden.isDead()) {
                        current.warden.remove();
                        activeWardens.remove(player.getUniqueId());
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), WARDEN_LIFETIME / 50);

            // Контроль дистанции
            startDistanceControl(player, newInfo);

            event.setCancelled(true);
        }
    }

    private void startDistanceControl(Player player, WardenInfo info) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (info.warden == null || info.warden.isDead()) {
                    this.cancel();
                    return;
                }

                // Автотелепорт если слишком далеко
                if (info.warden.getLocation().distance(player.getLocation()) > FOLLOW_RADIUS) {
                    info.warden.teleport(player.getLocation());
                }

                // Сбрасываем гнев на владельца
                if (info.warden.getAnger(player) > 0) {
                    info.warden.setAnger(player, 0);
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 40L);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Warden) {
            Warden warden = (Warden) event.getEntity();

            for (WardenInfo info : activeWardens.values()) {
                if (info.warden != null && info.warden.equals(warden)) {
                    // Запрещаем атаку на владельца
                    if (event.getTarget() instanceof Player &&
                        ((Player) event.getTarget()).getUniqueId().equals(info.ownerId)) {
                        event.setCancelled(true);
                        return;
                    }

                    // Разрешаем атаку только если цель совпадает с заданной
                    if (info.target != null && !event.getTarget().equals(info.target)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Владелец не может бить своего вардена
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Warden) {
            Player player = (Player) event.getDamager();
            Warden warden = (Warden) event.getEntity();

            WardenInfo info = activeWardens.get(player.getUniqueId());
            if (info != null && info.warden != null && info.warden.equals(warden)) {
                event.setCancelled(true);
                player.sendMessage("§cНельзя бить своего вардена!");
                return;
            }
        }

        // Варден не может бить владельца
        if (event.getDamager() instanceof Warden && event.getEntity() instanceof Player) {
            Warden warden = (Warden) event.getDamager();
            Player player = (Player) event.getEntity();

            for (WardenInfo info : activeWardens.values()) {
                if (info.warden != null && info.warden.equals(warden) &&
                    player.getUniqueId().equals(info.ownerId)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // УДАР ПОСОХОМ - смена цели
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player player = (Player) event.getDamager();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (isHypnosisStaff(item)) {
                WardenInfo info = activeWardens.get(player.getUniqueId());
                if (info != null && info.warden != null && !info.warden.isDead()) {
                    LivingEntity newTarget = (LivingEntity) event.getEntity();

                    if (newTarget.equals(player) || newTarget instanceof Warden) {
                        return;
                    }

                    // СБРАСЫВАЕМ ГНЕВ СО СТАРОЙ ЦЕЛИ
                    if (info.target != null && !info.target.isDead()) {
                        info.warden.setAnger(info.target, 0);
                    }

                    // ДОБАВЛЯЕМ ГНЕВ НОВОЙ ЦЕЛИ
                    info.warden.setAnger(newTarget, ANGER_AMOUNT);
                    
                    // УСТАНАВЛИВАЕМ НОВУЮ ЦЕЛЬ
                    info.target = newTarget;
                    info.warden.setTarget(newTarget);
                    
                    player.sendMessage("§5Варден перенаправлен на новую цель: " + 
                        (newTarget instanceof Player ? newTarget.getName() : "моб"));

                    // Эффекты
                    if (info.target != null) {
                        info.target.getWorld().spawnParticle(Particle.SCULK_SOUL,
                            info.target.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
                    }
                    newTarget.getWorld().playSound(newTarget.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
                }
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