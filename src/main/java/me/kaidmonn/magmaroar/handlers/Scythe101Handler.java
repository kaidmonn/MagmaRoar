package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public class Scythe101Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;

        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 101) return;

        if (MagmaRoar.getTeamManager().isTeammate(damager, victim)) return;
        if (damager.hasCooldown(Material.NETHERITE_HOE)) return;

        boolean stolen = false;
        List<Player> team = MagmaRoar.getTeamManager().getTeamMembers(damager);

        for (PotionEffect effect : victim.getActivePotionEffects()) {
            for (Player member : team) member.addPotionEffect(effect);
            victim.removePotionEffect(effect.getType());
            stolen = true;
        }

        if (stolen) damager.setCooldown(Material.NETHERITE_HOE, 65 * 20);
    }
}