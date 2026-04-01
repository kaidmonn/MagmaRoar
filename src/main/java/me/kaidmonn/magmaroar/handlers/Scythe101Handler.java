package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;

public class Scythe101Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;

        var item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 101) return;

        // Проверка кулдауна (шторка)
        if (damager.hasCooldown(Material.NETHERITE_HOE)) return;

        // Не работает на тимейтах
        if (MagmaRoar.getTeamManager().isTeammate(damager, victim)) {
            e.setCancelled(true);
            return;
        }

        boolean stolen = false;
        var team = MagmaRoar.getTeamManager().getTeamMembers(damager);

        for (PotionEffect effect : victim.getActivePotionEffects()) {
            for (Player member : team) {
                member.addPotionEffect(effect);
            }
            victim.removePotionEffect(effect.getType());
            stolen = true;
        }

        if (stolen) {
            // Установка кулдауна 65 секунд
            damager.setCooldown(Material.NETHERITE_HOE, 65 * 20);
        }
    }
}