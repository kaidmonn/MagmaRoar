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
    private final Map<UUID, Integer> aimingTicks = new HashMap<>();

    private static final long COOLDOWN = 45 * 1000;
    private static final int DURATION = 30 * 20;
    private static final int AIM_TIME = 60; // 3 секунды

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isShrinker(item)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Проверка кулдауна (после окончания эффектов)
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long secondsLeft = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cУменьшитель перезаряжается! Осталось: " + secondsLeft + " сек.");
            return;
        }

        // Shift+ЛКМ - уменьшить себя
        if (player.isSneaking() && event.getAction() == Action.LEFT_CLICK_AIR) {
            shrinkSelf(player);
            return;
        }

        // ПКМ - начало прицеливания
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            startAiming(player);
        }
    }

    private void shrinkSelf(Player player) {
        UUID uuid = player.getUniqueId();

        if (shrunkPlayers.containsKey(uuid)) {
            player.sendMessage("§cВы уже уменьшены!");
            return;
        }

        originalScale.put(uuid, 1.0);
        shrunkPlayers.put(uuid, true);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "attribute " + player.getName() + " minecraft:scale base set 0.5");

        player.sendMessage("§aВы уменьшились! (30 сек)");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);

        // КУЛДАУН СТАВИТСЯ ПОСЛЕ ВОЗВРАТА
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
                    
                    // КУЛДАУН НАЧИНАЕТСЯ ПОСЛЕ ВОЗВРАТА
                    cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN);
                    player.sendMessage("§6Уменьшитель перезаряжается 45 секунд.");
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), DURATION);
    }

    private void startAiming(Player player) {
        UUID uuid = player.getUniqueId();

        if (aimingTicks.containsKey(uuid)) {
            return;
        }

        player.sendMessage("§eПрицельтесь в игрока и держите ПКМ 3 секунды...");
        
        aimingTicks.put(uuid, 0);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!aimingTicks.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                
                int ticks = aimingTicks.get(uuid);
                
                if (!player.isSneaking() && !player.isBlocking()) {
                    ticks++;
                    aimingTicks.put(uuid, ticks);
                    
                    if (ticks % 20 == 0 && ticks < AIM_TIME) {
                        int secondsLeft = (AIM_TIME - ticks) / 20;
                        player.sendMessage("§eОсталось: " + secondsLeft + " сек...");
                    }
                    
                    if (ticks >= AIM_TIME) {
                        aimingTicks.remove(uuid);
                        Player target = getTargetPlayer(player, 15);
                        if (target != null && !target.equals(player)) {
                            enlargeTarget(player, target);
                        } else {
                            player.sendMessage("§cНет цели в прицеле!");
                        }
                        this.cancel();
                    }
                } else {
                    aimingTicks.remove(uuid);
                    player.sendMessage("§cПрицеливание прервано!");
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private Player getTargetPlayer(Player player, double range) {
        Player closest = null;
        double closestAngle = 0.95;
        
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                Vector toTarget = target.getLocation().toVector().subtract(player.getEyeLocation().toVector());
                Vector direction = player.getEyeLocation().getDirection();
                double dot = toTarget.normalize().dot(direction);
                
                if (dot > closestAngle) {
                    closestAngle = dot;
                    closest = target;
                }
            }
        }
        return closest;
    }

    private void enlargeTarget(Player owner, Player target) {
        UUID ownerId = owner.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        if (enlargedPlayers.containsKey(targetId)) {
            owner.sendMessage("§cЭтот игрок уже увеличен!");
            return;
        }
        
        originalScale.put(targetId, 1.0);
        originalMaxHealth.put(targetId, target.getAttribute(Attribute.MAX_HEALTH).getValue());
        enlargedPlayers.put(targetId, true);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "attribute " + target.getName() + " minecraft:scale base set 2.0");
        
        target.getAttribute(Attribute.MAX_HEALTH).setBaseValue(16);
        if (target.getHealth() > 16) {
            target.setHealth(16);
        }
        
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION, 2));
        
        target.sendMessage("§c§lВЫ УВЕЛИЧЕНЫ! (30 сек)");
        target.sendMessage("§cВаше здоровье: 8❤, вы медленнее!");
        owner.sendMessage("§aВы увеличили " + target.getName() + "!");
        
        target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.5f);
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation(), 100, 1, 2, 1, 0.5);
        
        // КУЛДАУН ПОСЛЕ ВОЗВРАТА
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
                    
                    // КУЛДАУН НАЧИНАЕТСЯ ПОСЛЕ ВОЗВРАТА
                    cooldowns.put(ownerId, System.currentTimeMillis() + COOLDOWN);
                    owner.sendMessage("§6Уменьшитель перезаряжается 45 секунд.");
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