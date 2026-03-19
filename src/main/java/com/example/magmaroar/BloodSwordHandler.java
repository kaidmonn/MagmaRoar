package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BloodSwordHandler implements Listener {

    private final Map<UUID, Integer> weaponMode = new HashMap<>(); 
    private final Map<UUID, Long> lastThrowTime = new HashMap<>();
    private final Map<UUID, UUID> thrownTridents = new HashMap<>(); // Ссылка: UUID трезубца -> UUID игрока
    
    private static final long THROW_COOLDOWN = 10 * 1000;

    // Числовые ID для CustomModelData
    private static final int MODEL_SWORD = 1001;
    private static final int MODEL_TRIDENT = 1002;
    private static final int MODEL_MACE = 1003;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (!isBloodWeapon(item)) return;

        // Shift + ПКМ — Переключение режима
        if (player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            int currentMode = weaponMode.getOrDefault(player.getUniqueId(), 0);
            int newMode = (currentMode + 1) % 3;
            weaponMode.put(player.getUniqueId(), newMode);
            
            updateWeapon(player, newMode);
            event.setCancelled(true);
            return;
        }

        // ПКМ (в режиме трезубца) — Бросок
        if (!player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
            if (item.getType() == Material.TRIDENT) {
                long now = System.currentTimeMillis();
                Long lastThrow = lastThrowTime.get(player.getUniqueId());
                
                if (lastThrow != null && now - lastThrow < THROW_COOLDOWN) {
                    long secondsLeft = (THROW_COOLDOWN - (now - lastThrow)) / 1000;
                    player.sendMessage("§cПерезарядка: " + secondsLeft + " сек.");
                    event.setCancelled(true);
                    return;
                }
                
                // Создаем снаряд
                Trident trident = player.launchProjectile(Trident.class);
                trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
                trident.setShooter(player);
                trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                trident.setGlowing(true);
                
                // Запоминаем, кто бросил
                thrownTridents.put(trident.getUniqueId(), player.getUniqueId());
                lastThrowTime.put(player.getUniqueId(), now);
                
                // Удаляем предмет из руки
                player.getInventory().setItemInMainHand(null);
                event.setCancelled(true);
            }
        }
    }

    private void updateWeapon(Player player, int mode) {
        Material mat;
        int model;
        String name;

        switch (mode) {
            case 1:
                mat = Material.TRIDENT;
                model = MODEL_TRIDENT;
                name = "§3Кровавый трезубец";
                break;
            case 2:
                mat = Material.MACE;
                model = MODEL_MACE;
                name = "§5Кровавая булава";
                break;
            default:
                mat = Material.NETHERITE_SWORD;
                model = MODEL_SWORD;
                name = "§cКровавый меч";
                break;
        }

        ItemStack newItem = createBloodItem(mat, model, name);
        player.getInventory().setItemInMainHand(newItem);
        player.sendMessage("§7Режим изменен на: " + name);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1f);
    }

    private ItemStack createBloodItem(Material material, int modelId, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            // Устанавливаем числовой Custom Model Data
            meta.setCustomModelData(modelId);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        UUID shooterUUID = thrownTridents.remove(trident.getUniqueId());
        if (shooterUUID == null) return;

        Player shooter = Bukkit.getPlayer(shooterUUID);
        if (shooter == null || !shooter.isOnline()) {
            trident.remove();
            return;
        }

        // Логика при попадании в сущность
        if (event.getHitEntity() != null) {
            Entity target = event.getHitEntity();
            target.teleport(shooter.getLocation().add(0, 1, 0));
            
            shooter.getWorld().spawnParticle(Particle.BLOOD_GUSH, target.getLocation(), 30, 0.5, 0.5, 0.5);
            shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
            shooter.sendMessage("§cЦель притянута!");
        }

        // Возвращаем меч игроку (модель 1001)
        ItemStack returnItem = createBloodItem(Material.NETHERITE_SWORD, MODEL_SWORD, "§cКровавый меч");
        Map<Integer, ItemStack> over = shooter.getInventory().addItem(returnItem);
        
        // Если инвентарь полон — выкидываем под ноги
        if (!over.isEmpty()) {
            over.values().forEach(i -> shooter.getWorld().dropItem(shooter.getLocation(), i));
        }

        trident.remove();
    }

    private boolean isBloodWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return false;
        int cmd = item.getItemMeta().getCustomModelData();
        return cmd == MODEL_SWORD || cmd == MODEL_TRIDENT || cmd == MODEL_MACE;
    }
}