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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StormBladeHandler implements Listener {

    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final Random random = new Random();
    
    private static final long ABILITY_COOLDOWN = 20 * 1000; // 20 секунд
    private static final double PASSIVE_CHANCE = 0.04; // 4% шанс
    private static final double WEAPON_DAMAGE = 14.0;
    private static final float EXPLOSION_POWER = 4.0f;
    private static final int LAUNCH_HEIGHT = 8;

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isStormBlade(item)) return;

        // Устанавливаем урон 14
        event.setDamage(WEAPON_DAMAGE);

        // Проверяем пассивный шанс 4%
        if (random.nextDouble() < PASSIVE_CHANCE) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                World world = target.getWorld();
                Location targetLoc = target.getLocation();
                
                // Подбрасываем на 8 блоков
                Vector velocity = target.getVelocity();
                velocity.setY(LAUNCH_HEIGHT * 0.4); // Примерно 8 блоков
                target.setVelocity(velocity);
                
                // Бьём молнией
                world.strikeLightningEffect(targetLoc);
                world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                
                // Визуальные эффекты
                world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                
                player.sendMessage("§b§lШТОРМ! Молния и подброс!");
                if (target instanceof Player) {
                    target.sendMessage("§cВас подбросило молнией!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isStormBlade(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            Long lastUse = abilityCooldowns.get(player.getUniqueId());
            
            if (lastUse != null && now - lastUse < ABILITY_COOLDOWN) {
                long secondsLeft = (ABILITY_COOLDOWN - (now - lastUse)) / 1000;
                player.sendMessage("§cСпособность перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }

            // Получаем точку взгляда
            Location targetLoc = player.getTargetBlock(null, 200).getLocation().add(0.5, 1, 0.5);
            World world = player.getWorld();

            player.sendMessage("§b§lКЛИНОК БУРИ! 10 молний! (кулдаун 20 сек)");
            
            // Звук начала
            world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);

            // Запускаем 10 молний с небольшими интервалами
            new BukkitRunnable() {
                int strikes = 0;
                
                @Override
                public void run() {
                    if (strikes >= 10) {
                        this.cancel();
                        return;
                    }
                    
                    // Небольшой разброс для каждой молнии
                    Location strikeLoc = targetLoc.clone().add(
                        (Math.random() - 0.5) * 3,
                        0,
                        (Math.random() - 0.5) * 3
                    );
                    
                    // Визуал молнии
                    world.strikeLightningEffect(strikeLoc);
                    
                    // ВЗРЫВ УРОВНЯ 4 (без разрушения блоков)
                    world.createExplosion(strikeLoc, EXPLOSION_POWER, false, true, player);
                    
                    // Дополнительные частицы
                    world.spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 30, 1, 1, 1, 0.1);
                    world.spawnParticle(Particle.FLASH, strikeLoc, 5, 1, 1, 1, 0);
                    
                    // Звук каждой молнии
                    world.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
                    
                    strikes++;
                }
            }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 2L); // Каждые 2 тика (быстро)

            abilityCooldowns.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    private boolean isStormBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Клинок бури");
    }
}