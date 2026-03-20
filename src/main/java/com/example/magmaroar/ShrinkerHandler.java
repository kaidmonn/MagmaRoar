package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ShrinkerHandler implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> shrunkPlayers = new HashMap<>();
    private final Map<UUID, Double> originalScale = new HashMap<>();
    private final Map<UUID, Double> originalMaxHealth = new HashMap<>();
    private final Map<UUID, Boolean> enlargedPlayers = new HashMap<>();
    private final Map<UUID, Boolean> aimingPlayers = new HashMap<>();

    private static final long COOLDOWN = 45 * 1000;
    private static final int DURATION = 30 * 20;
    private static final int AIM_TIME = 60; // 3 секунды (20 тиков * 3)

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isShrinker(item)) return;

        // Shift+ЛКМ - уменьшить себя
        if (player.isSneaking() && event.getAction() == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            shrinkSelf(player);
            return;
        }

        // ПКМ - начало прицеливания (зажатие)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            startAiming(player);
            event.setCancelled(true);
        }
    }

    private void shrinkSelf(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long secondsLeft = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cУменьшитель перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        if (shrunkPlayers.containsKey(uuid)) {
            player.sendMessage("§cВы уже уменьшены!");
            return;
        }

        originalScale.put(uuid, 1.0);
        shrunkPlayers.put(uuid, true);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "attribute " + player.getName() + " minecraft:scale base set 0.5");

        player.sendMessage("§aВы уменьшились до 0.5 блока! (30 сек)");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);

        // Таймер возврата
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shrunkPlayers.containsKey(uuid)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "attribute " + player.getName() + " minecraft:scale base set 1.0");
                    shrunkPlayers.remove(uuid);
                    originalScale.remove(uuid);
                    player.sendMessage("§cВы вернулись к нормальному размеру!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    
                    // КУЛДАУН ПОСЛЕ ВОЗВРАТА
                    cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), DURATION);
    }

    private void startAiming(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long secondsLeft = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cУменьшитель перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        if (aimingPlayers.containsKey(uuid)) {
            player.sendMessage("§cВы уже прицеливаетесь!");
            return;
        }

        player.sendMessage("§eЗажмите ПКМ на 3 секунды, прицелившись в игрока...");
        
        aimingPlayers.put(uuid, true);
        
        // Запускаем таймер на 3 секунды
        new BukkitRunnable() {
            int ticks = 0;
            boolean released = false;
            
            @Override
            public void run() {
                ticks++;
                
                // Проверяем, отпустил ли игрок ПКМ
                if (!player.isSneaking() && !player.isBlocking()) {
                    if (!released) {
                        released = true;
                        aimingPlayers.remove(uuid);
                        
                        if (ticks >= AIM_TIME) {
                            // Успешное прицеливание (прожал 3 секунды)
                            Player target = getTargetPlayer(player, 15);
                            if (target != null && !target.equals(player)) {
                                enlargeTarget(player, target);
                            } else {
                                player.sendMessage("§cНет цели в прицеле!");
                            }
                        } else {
                            // Отпустил раньше времени
                            player.sendMessage("§cВы отпустили ПКМ раньше времени! Нужно держать 3 секунды.");
                        }
                        this.cancel();
                        return;
                    }
                }
                
                // Если прошло 3 секунды и всё ещё зажат
                if (ticks >= AIM_TIME && !released) {
                    released = true;
                    aimingPlayers.remove(uuid);
                    
                    Player target = getTargetPlayer(player, 15);
                    if (target != null && !target.equals(player)) {
                        enlargeTarget(player, target);
                    } else {
                        player.sendMessage("§cНет цели в прицеле!");
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private Player getTargetPlayer(Player player, double range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                Vector toTarget = target.getLocation().toVector().subtract(player.getEyeLocation().toVector());
                Vector direction = player.getEyeLocation().getDirection();
                
                if (toTarget.normalize().dot(direction) > 0.8) {
                    return target;
                }
            }
        }
        return null;
    }

    private void enlargeTarget(Player owner, Player target) {
        UUID targetId = target.getUniqueId();
        
        if (enlargedPlayers.containsKey(targetId)) {
            owner.sendMessage("§cЭтот игрок уже увеличен!");
            return;
        }
        
        originalScale.put(targetId, 1.0);
        originalMaxHealth.put(targetId, target.getAttribute(Attribute.MAX_HEALTH).getValue());
        enlargedPlayers.put(targetId, true);

        // Увеличиваем до 4 блоков (scale 2.0)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "attribute " + target.getName() + " minecraft:scale base set 2.0");
        
        // Устанавливаем 8 сердец (16 HP)
        target.getAttribute(Attribute.MAX_HEALTH).setBaseValue(16);
        if (target.getHealth() > 16) {
            target.setHealth(16);
        }
        
        // Замедление
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION, 2));
        
        target.sendMessage("§c§lВЫ УВЕЛИЧЕНЫ ДО 4 БЛОКОВ! (30 сек)");
        target.sendMessage("§cВаше здоровье: 8❤, вы сильно медленнее!");
        owner.sendMessage("§aВы увеличили " + target.getName() + "!");
        
        target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.5f);
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation(), 100, 1, 2, 1, 0.5);
        
        // Таймер возврата
        new BukkitRunnable() {
            @Override
            public void run() {
                if (enlargedPlayers.containsKey(targetId)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "attribute " + target.getName() + " minecraft:scale base set 1.0");
                    target.getAttribute(Attribute.MAX_HEALTH).setBaseValue(originalMaxHealth.getOrDefault(targetId, 20.0));
                    if (target.getHealth() > target.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                    }
                    target.removePotionEffect(PotionEffectType.SLOWNESS);
                    enlargedPlayers.remove(targetId);
                    originalScale.remove(targetId);
                    originalMaxHealth.remove(targetId);
                    target.sendMessage("§aВы вернулись к нормальному размеру!");
                    target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);
                    
                    // КУЛДАУН ПОСЛЕ ВОЗВРАТА
                    cooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + COOLDOWN);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), DURATION);
    }

    private boolean isShrinker(ItemStack item) {
        if (item == null || item.getType() != Material.SPYGLASS) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        return ChatColor.stripColor(meta.getDisplayName()).contains("Уменьшитель");
    }
}