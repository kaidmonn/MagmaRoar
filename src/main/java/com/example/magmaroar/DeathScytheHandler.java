package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathScytheHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 40 * 1000; // 40 секунд
    private static final double DAMAGE = 10.0; // 5 сердец (10 HP)
    private static final double HEAL = 10.0; // 5 сердец (10 HP)

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
            // Если кулдаун не прошёл, обычный урон мотыги (1)
            event.setDamage(1.0);
            player.sendMessage("§cКоса смерти перезаряжается! Осталось: " + ((COOLDOWN - (now - lastUse)) / 1000) + " сек.");
            return;
        }

        // Отменяем обычный урон
        event.setCancelled(true);
        
        // Наносим истинный урон (игнорирует броню)
        target.damage(DAMAGE, player);
        
        // Лечим владельца
        double newHealth = Math.min(player.getHealth() + HEAL, player.getMaxHealth());
        player.setHealth(newHealth);
        
        // Ставим кулдаун
        cooldowns.put(player.getUniqueId(), now);
        
        // Сообщения
        player.sendMessage("§8§lКоса смерти вытянула жизнь! +5 сердец");
        if (target instanceof Player) {
            target.sendMessage("§c§lКоса смерти вытянула из вас жизнь!");
        }
        
        // Эффекты на жертве
        World world = target.getWorld();
        Location loc = target.getLocation().add(0, 1, 0);
        
        // Звук визера (тихий)
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.0f);
        
        // Частицы ходьбы по песку душ (круговые)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) { // 2 секунды
                    this.cancel();
                    return;
                }
                
                // Круговые частицы душ
                for (int i = 0; i < 360; i += 30) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, 
                        loc.clone().add(x, 1 + Math.sin(ticks * 0.3) * 0.5, z), 
                        1, 0, 0, 0, 0);
                }
                
                // Красные частицы урона
                world.spawnParticle(Particle.DAMAGE_INDICATOR, 
                    loc, 5, 0.5, 0.5, 0.5, 0.1);
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        
        // Частицы на владельце (исцеление)
        player.getWorld().spawnParticle(Particle.HEART, 
            player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
    }

    private boolean isDeathScythe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Коса смерти");
    }
}