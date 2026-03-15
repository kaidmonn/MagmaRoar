package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReaperScytheHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 80 * 1000; // 80 секунд

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isReaperScythe(item)) return;

        // Отменяем урон (коса не бьёт)
        event.setCancelled(true);
        
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        
        if (lastUse != null && now - lastUse < COOLDOWN) {
            long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
            player.sendMessage("§cКоса жнеца перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        // Кража эффектов
        stealEffects(target, player);
        
        // Ставим кулдаун
        cooldowns.put(player.getUniqueId(), now);
        
        // Сообщения
        player.sendMessage("§5§lКоса жнеца вытянула эффекты цели!");
        if (target instanceof Player) {
            target.sendMessage("§cКоса жнеца вытянула из вас все эффекты!");
        }
        
        // Визуальные эффекты
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.5f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
                    target.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0.02);
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void stealEffects(LivingEntity from, Player to) {
        // Копируем все положительные эффекты с цели
        for (PotionEffect effect : from.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            
            // Проверяем, является ли эффект положительным
            if (isBeneficial(type)) {
                // Добавляем владельцу
                to.addPotionEffect(new PotionEffect(type, 
                    effect.getDuration(), 
                    effect.getAmplifier(), 
                    effect.isAmbient(), 
                    effect.hasParticles(), 
                    effect.hasIcon()));
                
                // Убираем с цели
                from.removePotionEffect(type);
            }
        }
    }

    private boolean isBeneficial(PotionEffectType type) {
        // Список положительных эффектов
        return type == PotionEffectType.SPEED ||
               type == PotionEffectType.HASTE ||
               type == PotionEffectType.STRENGTH ||
               type == PotionEffectType.JUMP_BOOST ||
               type == PotionEffectType.REGENERATION ||
               type == PotionEffectType.RESISTANCE ||
               type == PotionEffectType.FIRE_RESISTANCE ||
               type == PotionEffectType.WATER_BREATHING ||
               type == PotionEffectType.INVISIBILITY ||
               type == PotionEffectType.NIGHT_VISION ||
               type == PotionEffectType.HEALTH_BOOST ||
               type == PotionEffectType.ABSORPTION ||
               type == PotionEffectType.SATURATION ||
               type == PotionEffectType.LUCK ||
               type == PotionEffectType.SLOW_FALLING ||
               type == PotionEffectType.CONDUIT_POWER ||
               type == PotionEffectType.DOLPHINS_GRACE ||
               type == PotionEffectType.HERO_OF_THE_VILLAGE;
    }

    private boolean isReaperScythe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_HOE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Коса жнеца");
    }
}