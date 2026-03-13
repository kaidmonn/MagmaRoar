package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpiderBladeHandler implements Listener {

    private final Map<UUID, Long> webCooldowns = new HashMap<>();
    private final Map<UUID, Set<Location>> placedWebs = new HashMap<>();
    private final Set<UUID> playersWithWebbing = new HashSet<>(); // Игроки с активным эффектом плетения
    private static final long WEB_COOLDOWN = 20 * 1000; // 20 секунд
    private static final double POISON_CHANCE = 0.07; // 7% шанс
    private static final int WEB_DURATION = 20 * 1000; // 20 секунд

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Проверяем при заходе, есть ли у игрока клинок
        checkPlayerWebbingEffect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем, есть ли у игрока клинок в руках
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        boolean hasBlade = isSpiderBlade(mainHand) || isSpiderBlade(offHand);
        UUID playerId = player.getUniqueId();
        
        if (hasBlade) {
            // Если клинок есть, даём эффект плетения
            if (!playersWithWebbing.contains(playerId)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, Integer.MAX_VALUE, 0, false, false, true));
                playersWithWebbing.add(playerId);
            }
        } else {
            // Если клинка нет, убираем эффект
            if (playersWithWebbing.contains(playerId)) {
                player.removePotionEffect(PotionEffectType.WEAVING);
                playersWithWebbing.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isSpiderBlade(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = webCooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < WEB_COOLDOWN) {
                long secondsLeft = (WEB_COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cПаутина перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // ГРОМКИЙ ЗВУК УБИЙСТВА ПАУКА
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_DEATH, 2.0f, 1.0f);
            
            // Создаём круг паутины 5×5 НА ПОВЕРХНОСТИ
            Location center = player.getLocation().clone();
            Set<Location> newWebs = new HashSet<>();
            
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    double distance = Math.sqrt(x*x + z*z);
                    if (distance <= 2.5) {
                        // Находим поверхность под игроком
                        Location webLoc = center.clone().add(x, 0, z);
                        
                        // Ищем первый блок снизу
                        while (webLoc.getBlock().getType() == Material.AIR && webLoc.getY() > -64) {
                            webLoc.subtract(0, 1, 0);
                        }
                        
                        // Ставим паутину на найденную поверхность
                        Location placeLoc = webLoc.clone().add(0, 1, 0);
                        if (placeLoc.getBlock().getType() == Material.AIR) {
                            placeLoc.getBlock().setType(Material.COBWEB);
                            newWebs.add(placeLoc.clone());
                        }
                    }
                }
            }
            
            // Сохраняем паутины для последующего удаления
            placedWebs.put(player.getUniqueId(), newWebs);
            webCooldowns.put(player.getUniqueId(), now);
            
            player.sendMessage("§2Паутина 5×5 создана на поверхности! Исчезнет через 20 секунд.");
            
            // Запускаем таймер на удаление паутины
            new BukkitRunnable() {
                @Override
                public void run() {
                    Set<Location> webs = placedWebs.remove(player.getUniqueId());
                    if (webs != null) {
                        for (Location loc : webs) {
                            if (loc.getBlock().getType() == Material.COBWEB) {
                                loc.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), WEB_DURATION / 50);
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isSpiderBlade(item)) return;

        if (Math.random() < POISON_CHANCE) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 400, 1));
            player.sendMessage("§2Яд сработал! Цель отравлена на 20 секунд.");
            
            target.getWorld().spawnParticle(Particle.ENTITY_EFFECT, 
                target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
        }
    }

    private void checkPlayerWebbingEffect(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        if (isSpiderBlade(mainHand) || isSpiderBlade(offHand)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, Integer.MAX_VALUE, 0, false, false, true));
            playersWithWebbing.add(player.getUniqueId());
        }
    }

    private boolean isSpiderBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Паучий клинок");
    }
}