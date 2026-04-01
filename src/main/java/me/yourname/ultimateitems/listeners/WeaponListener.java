package me.yourname.ultimateitems.listeners;

import me.yourname.ultimateitems.UltimateItems;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack; // Исправлено: добавлен импорт
import org.bukkit.metadata.FixedMetadataValue;
import java.util.*;

public class WeaponListener implements Listener {

    private final Map<UUID, Integer> excaliburCharges = new HashMap<>();

    // Вспомогательная проверка на тимейта
    private boolean isTeammate(Player p1, Entity e2) {
        if (!(e2 instanceof Player p2)) return false;
        if (UltimateItems.getInstance().getTeamManager().getTeam(p1) == null) return false;
        return UltimateItems.getInstance().getTeamManager().getTeam(p1).getMembers().contains(p2.getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity victim)) return;
        
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        int cmd = item.getItemMeta().getCustomModelData();
        
        // Защита тимейтов (на них способности не работают)
        if (isTeammate(attacker, victim)) {
            event.setCancelled(true);
            return;
        }

        switch (cmd) {
            case 101: // Коса 1: Кража эффектов
                if (attacker.getCooldown(item.getType()) <= 0) {
                    for (PotionEffect effect : victim.getActivePotionEffects()) {
                        attacker.addPotionEffect(effect);
                        UltimateItems.getInstance().getTeamManager().applyToTeam(attacker, effect);
                        victim.removePotionEffect(effect.getType());
                    }
                    attacker.setCooldown(item.getType(), 1300); // 65 сек
                }
                break;

            case 102: // Коса 2: Урон + Иссушение + Кровотечение
                if (attacker.getCooldown(item.getType()) <= 0) {
                    victim.damage(10); // 5 сердец
                    attacker.setHealth(Math.min(20, attacker.getHealth() + 4));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                    // Эффект кровотечения (запрет регена)
                    victim.setMetadata("bleeding", new FixedMetadataValue(UltimateItems.getInstance(), true));
                    Bukkit.getScheduler().runTaskLater(UltimateItems.getInstance(), () -> victim.removeMetadata("bleeding", UltimateItems.getInstance()), 300L);
                    attacker.setCooldown(item.getType(), 1500); // 75 сек
                }
                break;

            case 109: // Трезубец Посейдона: Молния 10%
                if (Math.random() < 0.1) {
                    victim.getWorld().strikeLightning(victim.getLocation());
                    victim.damage(6.0);
                }
                break;
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity().hasMetadata("bleeding")) {
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || 
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;
        
        int cmd = item.getItemMeta().getCustomModelData();

        // Катана 105 (Телепорт)
        if (cmd == 105 && (event.getAction().name().contains("RIGHT"))) {
            if (p.getCooldown(item.getType()) <= 0) {
                Location target = p.getTargetBlock(null, 15).getLocation().add(0, 1, 0);
                p.teleport(target);
                p.setCooldown(item.getType(), 300); // 15 сек
            }
        }

        // Легкая булава 106 (Подброс)
        if (cmd == 106 && (event.getAction().name().contains("RIGHT"))) {
            if (p.getCooldown(item.getType()) <= 0) {
                p.setVelocity(new Vector(0, 2.0, 0));
                p.setMetadata("no_fall", new FixedMetadataValue(UltimateItems.getInstance(), true));
                p.setCooldown(item.getType(), 400); // 20 сек
            }
        }

        // Экскалибур 204 (Заряды неуязвимости)
        if (cmd == 204 && (event.getAction().name().contains("RIGHT"))) {
            if (p.getCooldown(item.getType()) <= 0) {
                excaliburCharges.put(p.getUniqueId(), 15);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
                p.setCooldown(item.getType(), 1300); // 65 сек
                // Сброс через 30 сек если не потратил
                Bukkit.getScheduler().runTaskLater(UltimateItems.getInstance(), () -> excaliburCharges.remove(p.getUniqueId()), 600L);
            }
        }
    }

    @EventHandler
    public void onTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        // Логика Булавы (отмена урона от падения)
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && p.hasMetadata("no_fall")) {
            event.setCancelled(true);
            p.removeMetadata("no_fall", UltimateItems.getInstance());
        }

        // Логика Экскалибура (неуязвимость на 15 хитов)
        if (excaliburCharges.containsKey(p.getUniqueId())) {
            int charges = excaliburCharges.get(p.getUniqueId());
            if (charges > 0) {
                event.setCancelled(true);
                excaliburCharges.put(p.getUniqueId(), charges - 1);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1);
                p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 5);
                if (charges - 1 <= 0) excaliburCharges.remove(p.getUniqueId());
            }
        }
    }
}