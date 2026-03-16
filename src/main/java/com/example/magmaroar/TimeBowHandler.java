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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TimeBowHandler implements Listener {

    private final Map<UUID, TimeMark> activeMarks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, List<Location>> recordedPaths = new HashMap<>();
    private final Map<UUID, Boolean> isRewinding = new HashMap<>();
    
    private static final long MARK_DURATION = 20 * 1000; // 20 секунд
    private static final long COOLDOWN = 50 * 1000; // 50 секунд
    private static final int RECORD_INTERVAL = 2;

    private static class TimeMark {
        UUID targetId;
        long markTime;
        long startTime;

        TimeMark(UUID targetId, long markTime, long startTime) {
            this.targetId = targetId;
            this.markTime = markTime;
            this.startTime = startTime;
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();

        if (!isTimeBow(bow)) return;

        // Проверяем, есть ли уже активная метка
        if (activeMarks.containsKey(player.getUniqueId())) {
            player.sendMessage("§cУ вас уже есть активная метка! Используйте Shift+ПКМ для возврата.");
            event.setCancelled(true);
            return;
        }

        // Проверяем кулдаун
        if (cooldowns.containsKey(player.getUniqueId())) {
            long remaining = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("§cЛук времени перезаряжается! Осталось: " + remaining + " сек.");
                event.setCancelled(true);
                return;
            }
        }

        // Разрешаем выстрел
        Arrow arrow = (Arrow) event.getProjectile();
        arrow.setCustomName("§bВременная стрела");
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player)) return;
        
        Player shooter = (Player) arrow.getShooter();
        ItemStack bow = shooter.getInventory().getItemInMainHand();

        if (!isTimeBow(bow)) return;
        
        // Проверяем, не на кулдауне ли уже
        if (cooldowns.containsKey(shooter.getUniqueId())) {
            long remaining = (cooldowns.get(shooter.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                shooter.sendMessage("§cЛук времени перезаряжается! Осталось: " + remaining + " сек.");
                return;
            }
        }
        
        // Проверяем, нет ли уже активной метки
        if (activeMarks.containsKey(shooter.getUniqueId())) {
            shooter.sendMessage("§cУ вас уже есть активная метка! Используйте Shift+ПКМ для возврата.");
            return;
        }
        
        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            
            // Сообщение цели
            if (target instanceof Player) {
                target.sendMessage("§c§lВНИМАНИЕ! §eВы поражены Луком времени! У вас 20 секунд до возврата в прошлое.");
            }
            
            // Ставим метку
            activeMarks.put(shooter.getUniqueId(), 
                new TimeMark(target.getUniqueId(), System.currentTimeMillis(), System.currentTimeMillis()));
            
            // Зелёное свечение
            target.setGlowing(true);
            
            shooter.sendMessage("§aМетка времени поставлена на " + (target instanceof Player ? target.getName() : "цель") + " (20 сек)");
            
            // Запускаем запись пути
            startRecording(target, shooter.getUniqueId());
            
            // Убираем свечение через 20 секунд (если не использовали)
            new BukkitRunnable() {
                @Override
                public void run() {
                    TimeMark mark = activeMarks.get(shooter.getUniqueId());
                    if (mark != null && mark.targetId.equals(target.getUniqueId())) {
                        target.setGlowing(false);
                        activeMarks.remove(shooter.getUniqueId());
                        recordedPaths.remove(target.getUniqueId());
                        shooter.sendMessage("§cМетка времени истекла.");
                        
                        if (target instanceof Player) {
                            target.sendMessage("§aМетка времени исчезла. Вы в безопасности... пока.");
                        }
                    }
                }
            }.runTaskLater(MagmaRoarPlugin.getInstance(), MARK_DURATION / 50);
        } else {
            // Промах - ставим кулдаун
            shooter.sendMessage("§cВы промахнулись! Лук времени перезаряжается 50 секунд.");
            cooldowns.put(shooter.getUniqueId(), System.currentTimeMillis() + COOLDOWN);
        }
    }

    private void startRecording(LivingEntity target, UUID shooterId) {
        List<Location> path = new ArrayList<>();
        recordedPaths.put(target.getUniqueId(), path);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead() || isRewinding.containsKey(target.getUniqueId())) {
                    this.cancel();
                    return;
                }
                
                // Записываем позицию
                path.add(target.getLocation().clone());
                
                // Проверяем, не пора ли остановить запись
                TimeMark mark = activeMarks.get(shooterId);
                if (mark == null || System.currentTimeMillis() - mark.markTime > MARK_DURATION) {
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, RECORD_INTERVAL);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isTimeBow(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                // Shift+ПКМ - активация возврата
                activateRewind(player);
                event.setCancelled(true);
            }
        }
    }

    private void activateRewind(Player player) {
        TimeMark mark = activeMarks.get(player.getUniqueId());
        
        if (mark == null) {
            player.sendMessage("§cНет активной метки!");
            return;
        }
        
        Entity target = findEntity(mark.targetId);
        if (target == null || !(target instanceof LivingEntity)) {
            player.sendMessage("§cЦель больше не существует!");
            activeMarks.remove(player.getUniqueId());
            recordedPaths.remove(mark.targetId);
            return;
        }
        
        LivingEntity livingTarget = (LivingEntity) target;
        List<Location> path = recordedPaths.get(target.getUniqueId());
        
        if (path == null || path.isEmpty()) {
            player.sendMessage("§cНет записи пути цели!");
            activeMarks.remove(player.getUniqueId());
            recordedPaths.remove(target.getUniqueId());
            return;
        }
        
        // Сообщение цели перед возвратом
        if (target instanceof Player) {
            target.sendMessage("§c§lВАС ВОЗВРАЩАЮТ В ПРОШЛОЕ!");
        }
        
        // Убираем свечение
        livingTarget.setGlowing(false);
        
        // Запускаем обратное движение
        startRewind(livingTarget, path, player);
        
        // Ставим кулдаун
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN);
        player.sendMessage("§b§lВРЕМЯ НАЗАД! Цель возвращается в прошлое!");
        
        // Убираем метку
        activeMarks.remove(player.getUniqueId());
        recordedPaths.remove(target.getUniqueId());
    }

    private void startRewind(LivingEntity target, List<Location> path, Player activator) {
        UUID targetId = target.getUniqueId();
        isRewinding.put(targetId, true);
        
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 0.5f);
        
        new BukkitRunnable() {
            int index = path.size() - 1;
            
            @Override
            public void run() {
                if (index < 0 || target.isDead()) {
                    isRewinding.remove(targetId);
                    
                    if (!target.isDead()) {
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 1.5f);
                        if (target instanceof Player) {
                            target.sendMessage("§aВозврат во времени завершён.");
                        }
                    }
                    
                    this.cancel();
                    return;
                }
                
                Location targetLoc = path.get(index);
                target.teleport(targetLoc);
                
                target.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 10, 0.3, 0.3, 0.3, 0.02);
                
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    targetPlayer.setAllowFlight(true);
                    targetPlayer.setFlying(true);
                }
                
                index--;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, RECORD_INTERVAL);
        
        disableTargetActions(target);
    }

    private void disableTargetActions(LivingEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRewinding.containsKey(target.getUniqueId()) || target.isDead()) {
                    this.cancel();
                    
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        targetPlayer.setAllowFlight(false);
                        targetPlayer.setFlying(false);
                    }
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity damager = (LivingEntity) event.getDamager();
            if (isRewinding.containsKey(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.getType() == Material.ENDER_PEARL) {
            if (isRewinding.containsKey(player.getUniqueId())) {
                player.sendMessage("§cЭндер-жемчуг не работает во время возврата времени!");
                event.setCancelled(true);
            }
        }
    }

    private Entity findEntity(UUID id) {
        for (World world : MagmaRoarPlugin.getInstance().getServer().getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(id)) {
                    return e;
                }
            }
        }
        return null;
    }

    private boolean isTimeBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лук времени");
    }
}