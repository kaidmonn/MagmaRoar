package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Scythe102Handler implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        var item = attacker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 102) return;

        if (victim instanceof Player vicP && MagmaRoar.getInstance().getTeamManager().isTeammate(attacker, vicP)) {
            e.setCancelled(true);
            return;
        }

        if (attacker.getCooldown(Material.NETHERITE_HOE) > 0) return;

        // Урон и Хил
        e.setDamage(10.0); // 5 сердец
        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 10.0));
        
        MagmaRoar.getInstance().getTeamManager().getTeamMembers(attacker).forEach(uuid -> {
            Player t = Bukkit.getPlayer(uuid);
            if (t != null && t != attacker) t.setHealth(Math.min(t.getMaxHealth(), t.getHealth() + 4.0));
        });

        // Эффекты
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1)); // Иссушение 2 (индекс 1)
        
        // Кровотечение (через метаданные)
        victim.setMetadata("bleeding", new FixedMetadataValue(MagmaRoar.getInstance(), true));
        Bukkit.getScheduler().runTaskLater(MagmaRoar.getInstance(), 
            () -> victim.removeMetadata("bleeding", MagmaRoar.getInstance()), 300); // 15 сек

        attacker.setCooldown(Material.NETHERITE_HOE, 1500); // 75 секунд
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (e.getEntity().hasMetadata("bleeding")) {
            // Блокируем только натуральную регенерацию (от еды)
            if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || 
                e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                e.setCancelled(true);
            }
        }
    }
}