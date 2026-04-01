package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Mjolnir103Handler implements Listener {

    @EventHandler
    public void onSwing(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!isMjolnir(item)) return;

        // Проверка на полный замах (стандартный кулдаун топора)
        if (p.getAttackCooldown() < 1.0) return;

        Location loc = e.getEntity().getLocation();
        loc.getWorld().strikeLightningEffect(loc);
        
        // Урон по области 4 блока (кроме тимейтов)
        for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (nearby instanceof LivingEntity target && nearby != p) {
                if (!MagmaRoar.getInstance().getTeamManager().isTeammate(p, (Player) (target instanceof Player ? target : null))) {
                    target.damage(5.0); // 2.5 сердца
                }
            }
        }
    }

    @EventHandler
    public void onThrow(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!isMjolnir(item) || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        if (p.getCooldown(Material.DIAMOND_AXE) > 0) return;

        int originalSlot = p.getInventory().getHeldItemSlot();
        ItemStack throwItem = item.clone();
        p.getInventory().setItemInMainHand(null);
        p.setCooldown(Material.DIAMOND_AXE, 600); // 30 сек

        ArmorStand stand = p.getWorld().spawn(p.getEyeLocation(), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.getEquipment().setItemInMainHand(throwItem);
            s.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        });

        new BukkitRunnable() {
            int distance = 0;
            boolean returning = false;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (returning) {
                    Vector toPlayer = p.getLocation().add(0, 1, 0).toVector().subtract(stand.getLocation().toVector()).normalize();
                    stand.teleport(stand.getLocation().add(toPlayer.multiply(1.5)));
                    
                    if (stand.getLocation().distance(p.getLocation()) < 2) {
                        returnMjolnir(p, throwItem, originalSlot);
                        stand.remove();
                        this.cancel();
                    }
                } else {
                    stand.teleport(stand.getLocation().add(p.getLocation().getDirection().multiply(1.5)));
                    stand.setRightArmPose(stand.getRightArmPose().add(0, 0.5, 0)); // Визуал вращения
                    distance++;

                    // Проверка столкновения с блоком
                    if (stand.getLocation().getBlock().getType().isSolid() || distance > 26) { // ~40 блоков
                        areaDamage(stand.getLocation(), p);
                        returning = true;
                        if (stand.getLocation().getBlock().getType().isSolid()) {
                            // Логика "поторчит 2 секунды"
                            this.cancel();
                            new BukkitRunnable() {
                                @Override public void run() { 
                                    // Запуск возврата после паузы в 2 сек
                                    new BukkitRunnable() { @Override public void run() { /* повтор логики возврата */ } }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
                                }
                            }.runTaskLater(MagmaRoar.getInstance(), 40);
                        }
                    }

                    // Проверка попадания в сущность
                    for (Entity entity : stand.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (entity instanceof LivingEntity target && entity != p && !hitEntities.contains(entity.getUniqueId())) {
                            areaDamage(target.getLocation(), p);
                            hitEntities.add(entity.getUniqueId());
                            returning = true;
                        }
                    }
                }
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
    }

    private void areaDamage(Location loc, Player owner) {
        loc.getWorld().strikeLightningEffect(loc);
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (e instanceof LivingEntity target && e != owner) {
                if (!MagmaRoar.getInstance().getTeamManager().isTeammate(owner, (Player) (target instanceof Player ? target : null))) {
                    target.damage(8.0); // 4 сердца
                    target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5));
                }
            }
        }
    }

    private void returnMjolnir(Player p, ItemStack item, int slot) {
        if (p.getInventory().getItem(slot) == null) {
            p.getInventory().setItem(slot, item);
        } else {
            p.getInventory().addItem(item);
        }
    }

    private boolean isMjolnir(ItemStack item) {
        return item != null && item.getType() == Material.DIAMOND_AXE && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 103;
    }
}