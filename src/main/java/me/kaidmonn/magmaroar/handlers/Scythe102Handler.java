package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Scythe102Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;

        var item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 102) return;
        if (damager.hasCooldown(Material.NETHERITE_HOE)) return;
        if (MagmaRoar.getTeamManager().isTeammate(damager, victim)) return;

        e.setDamage(10.0);
        double maxH = damager.getAttribute(Attribute.MAX_HEALTH).getValue();
        damager.setHealth(Math.min(maxH, damager.getHealth() + 10.0));

        // ИСПРАВЛЕНО: Убран лишний getPlayer()
        for (Player member : MagmaRoar.getTeamManager().getTeamMembers(damager)) {
            if (member.equals(damager)) continue;
            double mMaxH = member.getAttribute(Attribute.MAX_HEALTH).getValue();
            member.setHealth(Math.min(mMaxH, member.getHealth() + 4.0));
        }

        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5 * 20, 1));
        damager.setCooldown(Material.NETHERITE_HOE, 75 * 20);
    }
}