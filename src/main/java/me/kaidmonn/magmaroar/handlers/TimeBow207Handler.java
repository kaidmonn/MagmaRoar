package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TimeBow207Handler implements Listener {

    private final Map<UUID, List<Location>> history = new HashMap<>();
    private final Map<UUID, UUID> ownerToVictim = new HashMap<>();

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player shooter)) return;
        if (!(e.getHitEntity() instanceof LivingEntity victim)) return;

        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (bow.getType() != Material.BOW || !bow.hasItemMeta() || bow.getItemMeta().getCustomModelData() != 207) return;

        if (shooter.getCooldown(Material.BOW) > 0) return;

        UUID vDir = victim.getUniqueId();
        ownerToVictim.put(shooter.getUniqueId(), vDir);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 0));

        // Запись истории (20 секунд = 200 записей при шаге в 2 тика)
        List<Location> locs = new ArrayList<>();
        history.put(vDir, locs);

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= 200 || !victim.isValid() || !ownerToVictim.containsKey(shooter.getUniqueId())) {
                    this.cancel();
                    return;
                }
                locs.add(victim.getLocation());
                i++;
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 2);

        shooter.setCooldown(Material.BOW, 800); // 40 сек
    }

    @EventHandler
    public void onRewind(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking() || !e.getAction().name().contains("LEFT_CLICK")) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() != Material.BOW || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 207) return;

        UUID victimUUID = ownerToVictim.remove(p.getUniqueId());
        if (victimUUID == null) return;

        Entity victim = org.bukkit.Bukkit.getEntity(victimUUID);
        List<Location> path = history.remove(victimUUID);

        if (victim instanceof LivingEntity lv && path != null) {
            Collections.reverse(path);
            new BukkitRunnable() {
                int step = 0;
                @Override
                public void run() {
                    if (step >= path.size() || !lv.isValid()) {
                        this.cancel();
                        return;
                    }
                    lv.teleport(path.get(step));
                    step += 2; // Перемотка чуть быстрее для эффекта
                }
            }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
        }
    }
}