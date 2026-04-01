package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Excalibur204Handler implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 204) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (p.getCooldown(Material.NETHERITE_SWORD) > 0 || p.hasMetadata("excalibur_active")) return;

        activateExcalibur(p);
    }

    private void activateExcalibur(Player p) {
        p.setMetadata("excalibur_active", new FixedMetadataValue(MagmaRoar.getInstance(), 15)); // 15 зарядов
        
        // Визуальный и звуковой эффект (гудение и частицы)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.hasMetadata("excalibur_active") || ticks >= 600) { // 30 сек
                    p.removeMetadata("excalibur_active", MagmaRoar.getInstance());
                    p.setCooldown(Material.NETHERITE_SWORD, 1300); // 65 сек
                    this.cancel();
                    return;
                }

                p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                if (ticks % 40 == 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 1f);
                }
                ticks += 2;
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 2);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!p.hasMetadata("excalibur_active")) return;

        int charges = p.getMetadata("excalibur_active").get(0).asInt();
        
        if (charges > 0) {
            e.setCancelled(true);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            
            if (charges - 1 <= 0) {
                p.removeMetadata("excalibur_active", MagmaRoar.getInstance());
                p.setCooldown(Material.NETHERITE_SWORD, 1300);
            } else {
                p.setMetadata("excalibur_active", new FixedMetadataValue(MagmaRoar.getInstance(), charges - 1));
            }
        }
    }
}