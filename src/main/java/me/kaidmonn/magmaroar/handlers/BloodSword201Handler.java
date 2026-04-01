package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class BloodSword201Handler implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        int cmd = item.getItemMeta().getCustomModelData();

        // Проверка, что это наше оружие (одна из 3-х моделей)
        if (cmd < 201 || cmd > 203) return;

        // ВЗАИМОДЕЙСТВИЕ С МЕЧОМ (201)
        if (cmd == 201 && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (p.isSneaking()) {
                if (p.getCooldown(Material.NETHERITE_SWORD) > 0) return;
                transform(item, 203, Material.MACE); // В Булаву
                addMaceEnchants(item);
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1f);
                startMaceTimer(p);
            } else {
                transform(item, 202, Material.TRIDENT); // В Трезубец
                p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);
            }
            return;
        }

        // ВЗАИМОДЕЙСТВИЕ С ТРЕЗУБЦЕМ (202)
        if (cmd == 202 && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (p.isSneaking()) {
                transform(item, 201, Material.NETHERITE_SWORD); // Назад в меч
            } else {
                launchTrident(p, item); // Бросок
            }
            e.setCancelled(true);
            return;
        }

        // ВЗАИМОДЕЙСТВИЕ С БУЛАВОЙ (203)
        if (cmd == 203 && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (!p.isSneaking()) {
                resetToSword(p, item);
                p.setCooldown(Material.NETHERITE_SWORD, 2400); // Кулдаун 120 сек
            }
        }
    }

    private void launchTrident(Player p, ItemStack item) {
        ItemStack displayItem = item.clone();
        p.getInventory().setItemInMainHand(null); // Убираем из рук на время полета

        ArmorStand stand = p.getWorld().spawn(p.getEyeLocation().subtract(0, 0.5, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false); // БЕЗ ГРАВИТАЦИИ
            s.getEquipment().setItemInMainHand(displayItem);
            s.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        });

        Vector dir = p.getLocation().getDirection().normalize();

        new BukkitRunnable() {
            int dist = 0;
            @Override
            public void run() {
                stand.teleport(stand.getLocation().add(dir.multiply(1.5)));
                dist++;

                // Поиск цели
                for (Entity target : stand.getNearbyEntities(1.2, 1.2, 1.2)) {
                    if (target instanceof LivingEntity living && target != p) {
                        living.teleport(p.getLocation()); // Тянем к себе
                        living.damage(5.0); // 2.5 сердца
                        p.playSound(p.getLocation(), Sound.ENTITY_ENCHANTED_GOLDEN_APPLE_UTTERANCE, 1f, 1f);
                        finishTridentFlight(p, stand, displayItem);
                        this.cancel();
                        return;
                    }
                }

                // Лимит 40 блоков (1.5 * 27 примерно 40) или блок
                if (dist > 27 || stand.getLocation().getBlock().getType().isSolid()) {
                    finishTridentFlight(p, stand, displayItem);
                    this.cancel();
                }
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
    }

    private void finishTridentFlight(Player p, ArmorStand stand, ItemStack item) {
        stand.remove();
        transform(item, 201, Material.NETHERITE_SWORD); // Превращаем обратно в меч
        p.getInventory().addItem(item); // Возвращаем в инвентарь
    }

    private void transform(ItemStack item, int cmd, Material mat) {
        item.setType(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
    }

    private void addMaceEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.BREACH, 4, true);
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        item.setItemMeta(meta);
    }

    private void startMaceTimer(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item != null && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 203) {
                    resetToSword(p, item);
                    p.setCooldown(Material.NETHERITE_SWORD, 2400);
                }
            }
        }.runTaskLater(MagmaRoar.getInstance(), 1800); // 90 сек
    }

    private void resetToSword(Player p, ItemStack item) {
        transform(item, 201, Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(Enchantment.BREACH);
        meta.removeEnchant(Enchantment.WIND_BURST);
        item.setItemMeta(meta);
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
    }
}