package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Random;

public class Trident109Handler implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.TRIDENT || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 109) return;

        // 10% шанс молнии
        if (random.nextInt(100) < 10) {
            if (e.getEntity() instanceof LivingEntity target) {
                target.getWorld().strikeLightningEffect(target.getLocation());
                target.damage(6.0); // 3 сердца чистого урона
            }
        }
    }

    @EventHandler
    public void onLaunch(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.TRIDENT || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 109) return;

        // Блокируем обычный бросок трезубца
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (p.isSneaking()) {
                if (p.getCooldown(Material.TRIDENT) > 0) return;

                // Логика "Тягуна" на суше
                Vector dir = p.getLocation().getDirection().normalize().multiply(3.0);
                p.setVelocity(dir);
                p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1f);
                
                // Защита от падения
                p.setMetadata("poseidon_jump", new FixedMetadataValue(MagmaRoar.getInstance(), true));
                
                p.setCooldown(Material.TRIDENT, 400); // 20 секунд
            }
            e.setCancelled(true); // Запрещаем кидать трезубец
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && p.hasMetadata("poseidon_jump")) {
            e.setCancelled(true);
            p.removeMetadata("poseidon_jump", MagmaRoar.getInstance());
        }
    }
}