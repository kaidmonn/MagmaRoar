package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathScytheHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 40 * 1000; // 40 секунд
    private static final double DAMAGE = 10.0; // 5 сердец

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isDeathScythe(item)) return;

        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());

        if (lastUse != null && now - lastUse < COOLDOWN) {
            // Обычный урон мотыги
            event.setDamage(1.0);
            return;
        }

        // Отменяем обычный урон
        event.setCancelled(true);
        
        // ПРЯМОЙ УРОН (игнорирует всё)
        double targetHealth = target.getHealth();
        double newHealth = targetHealth - DAMAGE;
        
        if (newHealth <= 0) {
            target.setHealth(0);
            target.damage(1); // Гарантированная смерть
        } else {
            target.setHealth(newHealth);
        }
        
        // Лечим владельца
        double playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double playerNewHealth = Math.min(player.getHealth() + DAMAGE, playerMaxHealth);
        player.setHealth(playerNewHealth);
        
        // Ставим кулдаун
        cooldowns.put(player.getUniqueId(), now);
        
        // Сообщения
        player.sendMessage("§8§lКоса смерти вытянула жизнь! +5 сердец");
        if (target instanceof Player) {
            target.sendMessage("§c§lКоса смерти вытянула из вас жизнь!");
        }
        
        // Минимальные эффекты
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.2f, 1.0f);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
            target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.HEART, 
            player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
    }

    private boolean isDeathScythe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Коса смерти");
    }
}