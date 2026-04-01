package me.kaidmonn.magmaroar.handlers;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class Katana105Handler implements Listener {

    @EventHandler
    public void onTeleport(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        var item = p.getInventory().getItemInMainHand();

        if (item.getType() == Material.NETHERITE_SWORD && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 105) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                if (p.hasCooldown(Material.NETHERITE_SWORD)) return;

                // Телепортация на 15 блоков вперед
                Vector dir = p.getLocation().getDirection().normalize().multiply(15);
                p.teleport(p.getLocation().add(dir));
                
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                
                // Кулдаун 15 секунд (300 тиков)
                p.setCooldown(Material.NETHERITE_SWORD, 15 * 20);
            }
        }
    }
}