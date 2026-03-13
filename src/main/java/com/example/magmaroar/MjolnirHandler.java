package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MjolnirHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN = 20 * 1000; // 20 секунд
    private final Map<UUID, ItemStack> thrownMjolnirs = new HashMap<>(); // Для возврата

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isMjolnir(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < COOLDOWN) {
                long secondsLeft = (COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cМьёльнир перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Сохраняем предмет перед броском
            ItemStack thrownItem = item.clone();
            
            // Удаляем предмет из руки
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            World world = player.getWorld();
            Location eyeLoc = player.getEyeLocation();
            Vector direction = player.getLocation().getDirection().normalize();
            
            // Создаём снежок как снаряд (лучше работает для возврата)
            Snowball projectile = world.spawn(eyeLoc, Snowball.class);
            projectile.setVelocity(direction.multiply(2.5));
            projectile.setGlowing(true);
            projectile.setShooter(player);
            
            // Сохраняем связь между снарядом и предметом
            thrownMjolnirs.put(projectile.getUniqueId(), thrownItem);
            
            cooldowns.put(player.getUniqueId(), now);
            player.sendMessage("§bМьёльнир брошен!");
            event.setCancelled(true);
            
            // Отслеживаем попадание
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (projectile == null || projectile.isDead() || projectile.isOnGround()) {
                        
                        if (projectile != null && !projectile.isDead()) {
                            Location hitLoc = projectile.getLocation();
                            
                            // Молния
                            world.strikeLightning(hitLoc);
                            
                            // Урон по области (3 сердца игноря броню)
                            for (Entity e : world.getNearbyEntities(hitLoc, 4, 2, 4)) {
                                if (e instanceof LivingEntity && !e.equals(player)) {
                                    LivingEntity target = (LivingEntity) e;
                                    target.damage(6, player); // 3 сердца
                                    
                                    // Эффекты молнии
                                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                                        target.getLocation(), 20, 1, 1, 1, 0.1);
                                }
                            }
                            
                            // Эффекты
                            world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 50, 2, 1, 2, 0.1);
                            world.spawnParticle(Particle.FLASH, hitLoc, 5, 1, 1, 1, 0);
                            world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                            
                            // ВОЗВРАТ МОЛОТА
                            ItemStack returningMjolnir = thrownMjolnirs.remove(projectile.getUniqueId());
                            if (returningMjolnir != null) {
                                // Даём предмет обратно
                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returningMjolnir);
                                if (!leftover.isEmpty()) {
                                    // Если инвентарь полон, выбрасываем на землю
                                    world.dropItemNaturally(player.getLocation(), returningMjolnir);
                                }
                                player.sendMessage("§bМьёльнир вернулся!");
                            }
                            
                            projectile.remove();
                        }
                        
                        this.cancel();
                    }
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
        }
    }

    private boolean isMjolnir(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null && 
               meta.displayName().toString().contains("Мьёльнир");
    }
}