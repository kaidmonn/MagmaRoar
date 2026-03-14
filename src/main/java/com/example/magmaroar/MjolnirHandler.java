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

public class MjolnirHandler implements Listener {

    private final Map<UUID, Long> meleeCooldowns = new HashMap<>();
    private final Map<UUID, Long> throwCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> thrownWeapons = new HashMap<>();
    private final Map<UUID, BukkitRunnable> returnTimers = new HashMap<>();

    private static final long MELEE_COOLDOWN = 3000;        // 3 секунды между ударами
    private static final long THROW_COOLDOWN = 20000;       // 20 секунд на бросок
    private static final long RETURN_DELAY = 10000;         // 10 секунд до автовозврата
    private static final double DAMAGE = 5.0;               // 2.5 сердца
    private static final int RADIUS = 5;                    // Радиус поражения

    // ====================== ЛКМ УДАР ======================
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMjolnir(item)) return;

        event.setCancelled(true); // Отменяем обычный урон

        long now = System.currentTimeMillis();
        Long lastMelee = meleeCooldowns.get(player.getUniqueId());

        if (lastMelee != null && now - lastMelee < MELEE_COOLDOWN) {
            player.sendMessage("§cМьёльнир перезаряжается! Подождите.");
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();
        Location center = target.getLocation();

        // Урон по области
        applyAreaDamage(player, center);

        // Молния (визуал)
        strikeVisualLightning(center);

        // Обновляем кулдаун
        meleeCooldowns.put(player.getUniqueId(), now);
    }

    // ====================== ПКМ БРОСОК ======================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isMjolnir(item)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        long now = System.currentTimeMillis();
        Long lastThrow = throwCooldowns.get(player.getUniqueId());

        if (lastThrow != null && now - lastThrow < THROW_COOLDOWN) {
            long secondsLeft = (THROW_COOLDOWN - (now - lastThrow)) / 1000;
            player.sendMessage("§cБросок перезаряжается! Осталось: " + secondsLeft + " сек.");
            event.setCancelled(true);
            return;
        }

        // Сохраняем предмет и удаляем из руки
        ItemStack thrownItem = item.clone();
        player.getInventory().setItemInMainHand(null);

        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();

        // Создаём снаряд (снежок)
        Snowball projectile = world.spawn(eyeLoc, Snowball.class);
        projectile.setVelocity(direction.multiply(2.5));
        projectile.setGlowing(true);
        projectile.setShooter(player);

        UUID projId = projectile.getUniqueId();
        thrownWeapons.put(projId, thrownItem);

        // Таймер автовозврата
        BukkitRunnable returnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (thrownWeapons.containsKey(projId)) {
                    returnMjolnir(player, projId);
                }
            }
        };
        returnTask.runTaskLater(MagmaRoarPlugin.getInstance(), RETURN_DELAY / 50);
        returnTimers.put(projId, returnTask);

        throwCooldowns.put(player.getUniqueId(), now);
        player.sendMessage("§bМьёльнир брошен!");
        event.setCancelled(true);
    }

    // ====================== ПОПАДАНИЕ СНАРЯДА ======================
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;

        Snowball snowball = (Snowball) event.getEntity();
        if (!(snowball.getShooter() instanceof Player)) return;

        Player player = (Player) snowball.getShooter();
        UUID projId = snowball.getUniqueId();

        if (!thrownWeapons.containsKey(projId)) return;

        // Отменяем таймер возврата
        BukkitRunnable timer = returnTimers.remove(projId);
        if (timer != null) timer.cancel();

        Location hitLoc = snowball.getLocation();

        // Урон по области в точке попадания
        applyAreaDamage(player, hitLoc);

        // Молния (визуал)
        strikeVisualLightning(hitLoc);

        // Возвращаем топор
        returnMjolnir(player, projId);
    }

    // ====================== УРОН ПО ОБЛАСТИ ======================
    private void applyAreaDamage(Player owner, Location center) {
        World world = center.getWorld();
        for (Entity e : world.getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
            if (e instanceof LivingEntity && !e.equals(owner)) {
                LivingEntity target = (LivingEntity) e;
                double newHealth = target.getHealth() - DAMAGE;
                if (newHealth <= 0) {
                    target.setHealth(0);
                    target.damage(1);
                } else {
                    target.setHealth(newHealth);
                }
            }
        }
    }

    // ====================== ВИЗУАЛ МОЛНИИ ======================
    private void strikeVisualLightning(Location loc) {
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.0f);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 50, 2, 2, 2, 0.1);
    }

    // ====================== ВОЗВРАТ ПРЕДМЕТА ======================
    private void returnMjolnir(Player player, UUID projId) {
        ItemStack item = thrownWeapons.remove(projId);
        if (item == null) return;

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        player.sendMessage("§b⚡ Мьёльнир вернулся!");
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Мьёльнир");
    }
}