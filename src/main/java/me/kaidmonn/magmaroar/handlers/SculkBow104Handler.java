package me.kaidmonn.magmaroar.handlers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SculkBow104Handler implements Listener {

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack bow = e.getBow();
        if (bow != null && bow.hasItemMeta() && bow.getItemMeta().getCustomModelData() == 104) {
            if (p.hasCooldown(Material.CROSSBOW)) {
                e.setCancelled(true);
                return;
            }
            Vector velocity = e.getProjectile().getVelocity();
            spawnExtraArrow(p, velocity, 10);
            spawnExtraArrow(p, velocity, -10);
            p.setCooldown(Material.CROSSBOW, 105 * 20);
        }
    }

    private void spawnExtraArrow(Player p, Vector baseVel, double angle) {
        Arrow arrow = p.launchProjectile(Arrow.class);
        double rad = Math.toRadians(angle);
        Vector newVel = new Vector(
            baseVel.getX() * Math.cos(rad) - baseVel.getZ() * Math.sin(rad),
            baseVel.getY(),
            baseVel.getX() * Math.sin(rad) + baseVel.getZ() * Math.cos(rad)
        );
        arrow.setVelocity(newVel);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        
        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (bow.hasItemMeta() && bow.getItemMeta().getCustomModelData() == 104) {
            Location loc = e.getHitBlock() != null ? e.getHitBlock().getLocation() : e.getEntity().getLocation();
            loc.getWorld().createExplosion(loc, 4.0f, false, false, shooter);
            
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Block b = loc.clone().add(x, y, z).getBlock();
                        if (b.getType().isSolid() && Math.random() < 0.6) b.setType(Material.SCULK);
                    }
                }
            }
            loc.getWorld().playSound(loc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1f, 0.5f);
        }
    }
}