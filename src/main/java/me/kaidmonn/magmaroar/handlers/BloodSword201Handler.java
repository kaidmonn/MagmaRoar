package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class BloodSword201Handler implements Listener {

    private final HashMap<UUID, Long> maceCooldown = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;
        int cmd = item.getItemMeta().getCustomModelData();

        // ЛОГИКА МЕЧА (201)
        if (cmd == 201) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (p.isSneaking()) {
                    // Шифт + ПКМ -> Булава (203)
                    if (isMaceOnCooldown(p)) {
                        p.sendMessage("§cБулава на кулдауне!");
                        return;
                    }
                    transformToMace(item);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 0.5f);
                    
                    // Авто-возврат через 90 сек
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 203) {
                                transformToSword(item);
                                startMaceCooldown(p);
                            }
                        }
                    }.runTaskLater(MagmaRoar.getInstance(), 90 * 20);
                } else {
                    // Просто ПКМ -> Трезубец (202)
                    transformToTrident(item);
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1.5f);
                }
            }
        }
        
        // ЛОГИКА ТРЕЗУБЦА (202)
        else if (cmd == 202) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (p.isSneaking()) {
                    // Шифт + ПКМ -> Обратно в Меч
                    transformToSword(item);
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
                } else {
                    // ПКМ -> Бросок
                    launchTrident(p, item);
                }
            }
        }

        // ЛОГИКА БУЛАВЫ (203)
        else if (cmd == 203) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!p.isSneaking()) {
                    // ПКМ -> Обратно в Меч
                    transformToSword(item);
                    startMaceCooldown(p);
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
                }
            }
        }
    }

    private void launchTrident(Player p, ItemStack trident) {
        Location start = p.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        ItemStack copy = trident.clone();
        trident.setAmount(0);

        ArmorStand as = start.getWorld().spawn(start, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.getEquipment().setItemInMainHand(copy);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        });

        new BukkitRunnable() {
            int dist = 0;
            boolean returning = false;

            @Override
            public void run() {
                if (!returning) {
                    as.teleport(as.getLocation().add(dir.clone().multiply(1.5)));
                    
                    for (Entity target : as.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (target instanceof LivingEntity le && !target.equals(p)) {
                            le.teleport(p.getLocation()); // Телепорт к владельцу
                            le.damage(5.0); // 2.5 сердца
                            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_HIT, 1f, 1f);
                            returning = true;
                            break;
                        }
                    }

                    if (as.getLocation().getBlock().getType().isSolid() || dist >= 26) {
                        returning = true;
                    }
                    dist++;
                } else {
                    Vector toP = p.getLocation().add(0, 1, 0).toVector().subtract(as.getLocation().toVector());
                    if (toP.length() < 1.5) {
                        transformToSword(copy);
                        p.getInventory().addItem(copy);
                        as.remove();
                        this.cancel();
                        return;
                    }
                    as.teleport(as.getLocation().add(toP.normalize().multiply(1.5)));
                }
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 1);
    }

    private void transformToSword(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(201);
        item.setItemMeta(meta);
        item.setType(Material.NETHERITE_SWORD);
        item.removeEnchantment(Enchantment.DENSITY);
        item.removeEnchantment(Enchantment.WIND_BURST);
    }

    private void transformToTrident(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(202);
        item.setItemMeta(meta);
        item.setType(Material.TRIDENT);
    }

    private void transformToMace(ItemStack item) {
        item.setType(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(203);
        meta.addEnchant(Enchantment.DENSITY, 4, true);
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        item.setItemMeta(meta);
    }

    private void startMaceCooldown(Player p) {
        maceCooldown.put(p.getUniqueId(), System.currentTimeMillis() + (120 * 1000));
        p.setCooldown(Material.MACE, 120 * 20);
    }

    private boolean isMaceOnCooldown(Player p) {
        return maceCooldown.containsKey(p.getUniqueId()) && maceCooldown.get(p.getUniqueId()) > System.currentTimeMillis();
    }
}