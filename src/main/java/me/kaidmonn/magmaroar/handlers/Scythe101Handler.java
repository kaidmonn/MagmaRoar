package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Scythe101Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 101) return;

        // Отмена урона по своим
        if (victim instanceof Player vicP && MagmaRoar.getInstance().getTeamManager().isTeammate(attacker, vicP)) {
            e.setCancelled(true);
            return;
        }

        if (attacker.getCooldown(Material.NETHERITE_HOE) > 0) return;

        // Логика кражи эффектов
        for (PotionEffect effect : victim.getActivePotionEffects()) {
            attacker.addPotionEffect(effect);
            // Раздаем тимейтам
            MagmaRoar.getInstance().getTeamManager().getTeamMembers(attacker).forEach(uuid -> {
                Player teammate = Bukkit.getPlayer(uuid);
                if (teammate != null) teammate.addPotionEffect(effect);
            });
            victim.removePotionEffect(effect.getType());
        }

        attacker.setCooldown(Material.NETHERITE_HOE, 1300); // 65 секунд
    }
}