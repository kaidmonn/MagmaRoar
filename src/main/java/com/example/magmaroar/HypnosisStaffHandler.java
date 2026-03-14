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
    private static final int ANGER_AMOUNT = 150; // +150 гнева при ударе посохом

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
            
            // Убираем эффект тьмы
            warden.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 0, 0)); // Сбрасываем

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

            // Запускаем контроль вардена
            startWardenControl(player, newInfo);

            event.setCancelled(true);
        }
    }

    private void startWardenControl(Player player, WardenInfo info) {
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
                    player.sendMessage("§5Варден телепортирован к вам (слишком далеко)");
                }

                // Сбрасываем гнев на владельца
                if (info.warden.getAnger(player) > 0) {
                    info.warden.setAnger(player, 0);
                }

                // Если есть цель
                if (info.target != null && !info.target.isDead()) {
                    // Проверяем дистанцию цели до владельца
                    if (info.target.getLocation().distance(player.getLocation()) > FOLLOW_RADIUS) {
                        info.target = null;
                        info.warden.setTarget(null);
                    } else {
                        info.warden.setTarget(info.target);
                    }
                }

                // Если нет цели - следуем за владельцем
                if (info.target == null || info.target.isDead()) {
                    if (info.warden.getLocation().distance(player.getLocation()) > 3) {
                        info.warden.setTarget(player);
                    } else {
                        info.warden.setTarget(null);
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 20L);
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

        // УДАР ПОСОХОМ - добавляем 150 гнева и задаём цель
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player player = (Player) event.getDamager();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (isHypnosisStaff(item)) {
                WardenInfo info = activeWardens.get(player.getUniqueId());
                if (info != null && info.warden != null && !info.warden.isDead()) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    if (target.equals(player) || target instanceof Warden) {
                        return;
                    }

                    // Добавляем 150 гнева цели
                    info.warden.setAnger(target, ANGER_AMOUNT);
                    
                    // Устанавливаем цель
                    info.target = target;
                    info.warden.setTarget(target);
                    
                    player.sendMessage("§5Варден в ярости! +150 гнева к цели!");

                    target.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        target.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        // Убираем эффект тьмы у наших варденов
        if (event.getEntity() instanceof Warden) {
            Warden warden = (Warden) event.getEntity();
            
            for (WardenInfo info : activeWardens.values()) {
                if (info.warden != null && info.warden.equals(warden) &&
                    event.getNewEffect() != null && 
                    event.getNewEffect().getType().equals(PotionEffectType.DARKNESS)) {
                    event.setCancelled(true);
                    return;
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