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
    private static final long COOLDOWN = 90 * 1000; // 90 секунд
    private static final int WARDEN_LIFETIME = 40 * 1000; // 40 секунд
    private static final int FOLLOW_RADIUS = 10; // Максимальная дистанция следования

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

            // Проверяем, есть ли уже активный Варден
            WardenInfo info = activeWardens.get(player.getUniqueId());

            if (info != null && info.warden != null && !info.warden.isDead()) {
                // Телепортируем Вардена к игроку
                info.warden.teleport(player.getLocation());
                player.sendMessage("§5Варден телепортирован к вам!");
                event.setCancelled(true);
                return;
            }

            // Проверка кулдауна
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЖезл гипноза перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Призыв Вардена
            Location spawnLoc = player.getLocation();
            World world = player.getWorld();

            Warden warden = world.spawn(spawnLoc, Warden.class);

            // Настройка Вардена (пониженный урон)
            warden.setAI(true);
            warden.setTarget(null);
            warden.setHealth(100); // Уменьшенное здоровье
            warden.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2)); // Скорость 3
            warden.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1)); // Сопротивление

            // Сохраняем информацию
            WardenInfo newInfo = new WardenInfo(warden, now, player.getUniqueId());
            activeWardens.put(player.getUniqueId(), newInfo);
            cooldowns.put(player.getUniqueId(), now);

            // Звук призыва
            world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
            player.sendMessage("§5Варден призван! Он будет следовать за вами 40 секунд.");

            // Запускаем таймер на исчезновение
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

            // Запускаем таймер на управление
            startWardenAI(player, newInfo);

            event.setCancelled(true);
        }
    }

    private void startWardenAI(Player player, WardenInfo info) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Проверяем, жив ли Варден и не истекло ли время
                if (info.warden == null || info.warden.isDead() ||
                    System.currentTimeMillis() - info.spawnTime > WARDEN_LIFETIME) {
                    this.cancel();
                    return;
                }

                // Если есть цель
                if (info.target != null && !info.target.isDead()) {
                    double distToTarget = info.warden.getLocation().distance(info.target.getLocation());
                    double distToOwner = info.warden.getLocation().distance(player.getLocation());

                    // Если цель дальше FOLLOW_RADIUS от владельца - забываем её
                    if (info.target.getLocation().distance(player.getLocation()) > FOLLOW_RADIUS) {
                        info.target = null;
                        info.warden.setTarget(null);
                    } else {
                        // Атакуем цель
                        info.warden.setTarget(info.target);
                    }
                }

                // Если нет цели или цель вне зоны - следуем за владельцем
                if (info.target == null || info.target.isDead()) {
                    double distToOwner = info.warden.getLocation().distance(player.getLocation());

                    if (distToOwner > 3) {
                        // Идём к владельцу
                        info.warden.setTarget(player);
                    } else {
                        // Стоим рядом
                        info.warden.setTarget(null);
                    }
                }

                // Частицы связи
                if (info.target != null) {
                    player.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        info.target.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02);
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 10L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isHypnosisStaff(item)) return;

        // Задаём цель для Вардена
        WardenInfo info = activeWardens.get(player.getUniqueId());
        if (info != null && info.warden != null && !info.warden.isDead()) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();

                // Нельзя атаковать владельца или другого Вардена
                if (target.equals(player) || target instanceof Warden) {
                    return;
                }

                info.target = target;
                player.sendMessage("§5Варден атакует: " + (target instanceof Player ? target.getName() : "моб"));

                // Эффект на цели
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