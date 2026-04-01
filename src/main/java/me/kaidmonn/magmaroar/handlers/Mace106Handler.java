package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Random;

public class Mace106Handler implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.MACE || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 106) return;

        // Логика шанса сохранения Wind Charge
        if (item.getType() == Material.WIND_CHARGE) {
             if (random.nextInt(100) < 40) {
                 // В Paper 1.21.4 отмена события здесь предотвратит трату заряда
                 e.setCancelled(true); 
                 return;
             }
        }

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (p.getCooldown(Material.MACE) > 0) return;

        // Прыжок на 20 блоков (Y вектор ~2.2 дает примерно такую высоту)
        p.setVelocity(new Vector(0, 2.2, 0));
        
        // Ставим метку защиты от падения
        p.setMetadata("mace_jump", new FixedMetadataValue(MagmaRoar.getInstance(), true));
        
        p.setCooldown(Material.MACE, 400); // 20 секунд
    }

    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (p.hasMetadata("mace_jump")) {
                e.setCancelled(true);
                p.removeMetadata("mace_jump", MagmaRoar.getInstance());
            }
        }
    }
}