package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExcaliburHandler implements Listener {

    private final Map<UUID, ShieldInfo> activeShields = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    private static final long COOLDOWN = 60 * 1000; // 60 секунд
    private static final int MAX_HITS = 20; // 20 ударов

    private static class ShieldInfo {
        int hitsLeft;
        long endTime;

        ShieldInfo(int hitsLeft, long endTime) {
            this.hitsLeft = hitsLeft;
            this.endTime = endTime;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isExcalibur(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            
            // Проверяем, не активен ли уже щит
            if (activeShields.containsKey(player.getUniqueId())) {
                player.sendMessage("§cЭкскалибур уже активен!");
                event.setCancelled(true);
                return;
            }
            
            // Проверяем кулдаун
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cЭкскалибур перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Активируем Экскалибур
            activeShields.put(player.getUniqueId(), new ShieldInfo(MAX_HITS, now + 30000)); // Макс 30 сек
            
            player.sendMessage("§6§lЭКСКАЛИБУР! Неуязвимость на " + MAX_HITS + " ударов!");
            
            // Запускаем визуальные эффекты
            startShieldEffects(player);
            
            event.setCancelled(true);
        }
    }

    private void startShieldEffects(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ShieldInfo info = activeShields.get(player.getUniqueId());
                
                // Если щит закончился
                if (info == null || info.hitsLeft <= 0 || System.currentTimeMillis() > info.endTime) {
                    
                    if (info != null) {
                        // Если закончился по ударам
                        if (info.hitsLeft <= 0) {
                            player.sendMessage("§6Экскалибур исчерпал свою защиту.");
                        } 
                        // Если закончился по времени
                        else if (System.currentTimeMillis() > info.endTime) {
                            player.sendMessage("§6Время Экскалибура истекло.");
                        }
                        
                        activeShields.remove(player.getUniqueId());
                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                    
                    this.cancel();
                    return;
                }
                
                // Частицы тотема вокруг игрока
                Location loc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 5, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ShieldInfo info = activeShields.get(player.getUniqueId());
        
        if (info != null && info.hitsLeft > 0) {
            // Отменяем урон
            event.setCancelled(true);
            
            // Уменьшаем счётчик
            info.hitsLeft--;
            
            // Звук наковальни
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
            
            // Визуальный эффект
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            
            // Сообщение
            if (info.hitsLeft > 0) {
                player.sendMessage("§6Экскалибур поглотил удар! Осталось: " + info.hitsLeft + "/" + MAX_HITS);
            } else {
                player.sendMessage("§cЭкскалибур исчерпал свою защиту!");
                activeShields.remove(player.getUniqueId());
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    private boolean isExcalibur(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Экскалибур");
    }
}