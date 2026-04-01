package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Mjolnir103Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        ItemStack item = damager.getInventory().getItemInMainHand();

        if (item.getType() == Material.DIAMOND_AXE && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 103) {
            if (!(e.getEntity() instanceof LivingEntity victim)) return;

            // Молния при полном замахе (стандартный кулдаун топора)
            if (damager.getAttackCooldown() >= 1.0) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                
                victim.getNearbyEntities(4, 4, 4).forEach(entity -> {
                    if (entity instanceof LivingEntity target && entity != damager) {
                        if (entity instanceof Player p && MagmaRoar.getTeamManager().isTeammate(damager, p)) return;
                        target.damage(5.0, damager); // 2.5 сердца чистого урона
                    }
                });
            }
        }
    }

    @EventHandler
    public void onThrow(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() == Material.DIAMOND_AXE && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 103) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (p.hasCooldown(Material.DIAMOND_AXE)) return;

                p.setCooldown(Material.DIAMOND_AXE, 30 * 20); // Кулдаун 30 сек
                ItemStack itemToThrow = item.clone();
                p.getInventory().setItemInMainHand(null);

                // Создаем визуал летящего молота
                ArmorStand as = p.getWorld().spawn(p.getEyeLocation(), ArmorStand.class, armorStand -> {
                    armorStand.setVisible(false);
                    armorStand.setMarker(true);
                    armorStand.getEquipment().setItemInMainHand(itemToThrow);
                    armorStand.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(-90), 0, 0));
                });

                new BukkitRunnable() {
                    int distance = 0;
                    boolean returning = false;
                    Vector direction = p.getLocation().getDirection().normalize();

                    @Override
                    public void run() {
                        if (returning) {
                            // Логика возврата к игроку
                            Vector toPlayer = p.getLocation().add(0, 1, 0).toVector().subtract(as.getLocation().toVector()).normalize();
                            as.teleport(as.getLocation().add(toPlayer.multiply(1.5)));

                            if (as.getLocation().distance(p.getLocation()) < 2) {
                                p.getInventory().addItem(itemToThrow);
                                p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
                                as.remove();
                                this.cancel();
                            }
                        } else {
                            // Полет вперед
                            as.teleport(as.getLocation().add(direction.multiply(1.5)));
                            as.setHeadPose(as.getHeadPose().add(0, 0.5, 0)); // Кручение
                            distance++;

                            // Проверка попадания в сущность
                            for (org.bukkit.entity.Entity target : as.getNearbyEntities(1.5, 1.5, 1.5)) {
                                if (target instanceof LivingEntity living && target != p) {
                                    if (target instanceof Player teammate && MagmaRoar.getTeamManager().isTeammate(p, teammate)) continue;
                                    
                                    living.getWorld().strikeLightningEffect(living.getLocation());
                                    living.damage(8.0, p); // 4 сердца
                                    
                                    // Откидывание
                                    Vector knockback = living.getLocation().toVector().subtract(as.getLocation().toVector()).normalize().multiply(1.5);
                                    living.setVelocity(knockback);
                                    
                                    returning = true;
                                }
                            }

                            // Попадание в блоки или макс. дистанция
                            if (distance > 26 || !as.getLocation().getBlock().getType().isAir()) {
                                if (!as.getLocation().getBlock().getType().isAir()) {
                                    // Если в блоке — висим 2 секунды
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() { returning = true; }
                                    }.runTaskLater(MagmaRoar.getInstance(), 40);
                                    this.cancel(); 
                                    // Перезапуск возврата после паузы в блоке
                                    startReturnTask(as, p, itemToThrow);
                                } else {
                                    returning = true;
                                }
                            }
                        }
                    }
                }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
            }
        }
    }

    private void startReturnTask(ArmorStand as, Player p, ItemStack item) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!as.isValid() || !p.isOnline()) { as.remove(); this.cancel(); return; }
                Vector toPlayer = p.getEyeLocation().toVector().subtract(as.getLocation().toVector()).normalize();
                as.teleport(as.getLocation().add(toPlayer.multiply(1.5)));
                if (as.getLocation().distance(p.getLocation()) < 2) {
                    p.getInventory().addItem(item);
                    as.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 40, 1);
    }
}