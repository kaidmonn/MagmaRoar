package me.yourname.ultimateitems.listeners;

import me.yourname.ultimateitems.UltimateItems;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public class WeaponListener implements Listener {

    private final Map<UUID, Integer> excaliburCharges = new HashMap<>();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity victim)) return;
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        int cmd = item.getItemMeta().getCustomModelData();
        
        // Защита тимейтов
        if (UltimateItems.getInstance().getTeamManager().getTeam(attacker) != null) {
            if (UltimateItems.getInstance().getTeamManager().getTeam(attacker).getMembers().contains(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // Коса 101 (Эффекты)
        if (cmd == 101 && attacker.getCooldown(item.getType()) <= 0) {
            for (PotionEffect pe : victim.getActivePotionEffects()) {
                attacker.addPotionEffect(pe);
                UltimateItems.getInstance().getTeamManager().applyToTeam(attacker, pe);
                victim.removePotionEffect(pe.getType());
            }
            attacker.setCooldown(item.getType(), 1300);
        }

        // Коса 102 (ХП + Иссушение)
        if (cmd == 102 && attacker.getCooldown(item.getType()) <= 0) {
            victim.damage(10);
            attacker.setHealth(Math.min(20, attacker.getHealth() + 4));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
            attacker.setCooldown(item.getType(), 1500);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;
        int cmd = item.getItemMeta().getCustomModelData();

        // Катана 105 (Телепорт)
        if (cmd == 105 && event.getAction().name().contains("RIGHT")) {
            if (p.getCooldown(item.getType()) <= 0) {
                p.teleport(p.getTargetBlock(null, 15).getLocation().add(0, 1, 0));
                p.setCooldown(item.getType(), 300);
            }
        }

        // Теневой клинок 107
        if (cmd == 107 && event.getAction().name().contains("RIGHT")) {
            if (p.getCooldown(item.getType()) <= 0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2));
                p.setCooldown(item.getType(), 1300);
                // Скрытие ника и брони (логика пакетов или hidePlayer)
            }
        }

        // Экскалибур 204
        if (cmd == 204 && event.getAction().name().contains("RIGHT")) {
            if (p.getCooldown(item.getType()) <= 0) {
                excaliburCharges.put(p.getUniqueId(), 15);
                p.setCooldown(item.getType(), 1300);
                p.sendMessage("§eЗащита Экскалибура активирована!");
            }
        }
    }

    @EventHandler
    public void onTakingDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && excaliburCharges.getOrDefault(p.getUniqueId(), 0) > 0) {
            event.setCancelled(true);
            int c = excaliburCharges.get(p.getUniqueId()) - 1;
            excaliburCharges.put(p.getUniqueId(), c);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
            if (c <= 0) excaliburCharges.remove(p.getUniqueId());
        }
    }
}