package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Random;

public class SculkBow104Handler implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack item = e.getBow();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 104) return;

        if (p.getCooldown(Material.CROSSBOW) > 0) {
            e.setCancelled(true);
            return;
        }

        // Логика тройного выстрела (как чары, но вручную)
        Vector baseVelocity = e.getProjectile().getVelocity();
        spawnSculkArrow(p, baseVelocity.clone().rotateAroundY(Math.toRadians(10)));
        spawnSculkArrow(p, baseVelocity.clone().rotateAroundY(Math.toRadians(-10)));
        
        // Помечаем основную стрелу
        e.getProjectile().setMetadata("sculk_arrow", new FixedMetadataValue(MagmaRoar.getInstance(), true));
        
        p.setCooldown(Material.CROSSBOW, 2100); // 105 сек
    }

    private void spawnSculkArrow(Player p, Vector velocity) {
        Arrow arrow = p.launchProjectile(Arrow.class, velocity);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setMetadata("sculk_arrow", new FixedMetadataValue(MagmaRoar.getInstance(), true));
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!e.getEntity().hasMetadata("sculk_arrow")) return;
        
        Location loc = e.getHitBlock() != null ? e.getHitBlock().getLocation() : e.getHitEntity().getLocation();
        
        // Взрыв уровня 4
        loc.getWorld().createExplosion(loc, 4.0f, false, false);
        
        // Заражение скалком (имитация 4 убитых мобов)
        infectWithSculk(loc, 3);
        
        e.getEntity().remove(); // Удаляем стрелу после взрыва
    }

    private void infectWithSculk(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (random.nextInt(100) > 40) continue; // Рандомное заполнение
                    
                    Block b = center.clone().add(x, y, z).getBlock();
                    if (b.getType().isSolid() && b.getType() != Material.BEDROCK) {
                        if (random.nextBoolean()) {
                            b.setType(Material.SCULK);
                        } else {
                            // Ставим жилы на поверхности
                            Block above = b.getRelative(0, 1, 0);
                            if (above.getType().isAir()) above.setType(Material.SCULK_VEIN);
                        }
                    }
                }
            }
        }
    }
}