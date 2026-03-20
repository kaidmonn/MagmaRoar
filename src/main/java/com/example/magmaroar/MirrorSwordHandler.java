package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MirrorSwordHandler implements Listener {

    private final Map<UUID, MirrorData> owners = new HashMap<>();
    private final Map<UUID, UUID> copyToOwner = new HashMap<>();
    
    private static final int MAX_COPIES = 5;
    private static final long BUFF_COOLDOWN = 60 * 1000;
    private static final int BUFF_DURATION = 20 * 20;

    private static class MirrorData {
        int copiesLeft = MAX_COPIES;
        List<UUID> copyHolders = new ArrayList<>();
        long lastBuffTime = 0;
        UUID ownerId;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isMirrorSword(item)) return;

        // ПКМ - бафф (без шифта)
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && !player.isSneaking()) {
            MirrorData data = owners.get(player.getUniqueId());
            if (data == null) {
                data = new MirrorData();
                data.ownerId = player.getUniqueId();
                owners.put(player.getUniqueId(), data);
            }
            
            long now = System.currentTimeMillis();
            if (now - data.lastBuffTime < BUFF_COOLDOWN) {
                long secondsLeft = (BUFF_COOLDOWN - (now - data.lastBuffTime)) / 1000;
                player.sendMessage("§cСпособность перезаряжается! Осталось: " + secondsLeft + " сек.");
                event.setCancelled(true);
                return;
            }
            
            // Бафф владельцу
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, BUFF_DURATION, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, BUFF_DURATION, 0));
            player.sendMessage("§aВы активировали бафф! Вы светитесь.");
            
            // Бафф всем с копиями
            for (UUID holderId : data.copyHolders) {
                Player holder = Bukkit.getPlayer(holderId);
                if (holder != null && holder.isOnline()) {
                    holder.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, BUFF_DURATION, 1));
                    holder.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, 2));
                    holder.sendMessage("§aВладелец Зеркального меча активировал бафф!");
                }
            }
            
            data.lastBuffTime = now;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player owner = event.getPlayer();
        ItemStack item = owner.getInventory().getItemInMainHand();
        
        if (!isMirrorSword(item)) return;
        if (!owner.isSneaking()) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player target = (Player) event.getRightClicked();
        
        if (owner.getLocation().distance(target.getLocation()) > 5) {
            owner.sendMessage("§cЦель слишком далеко!");
            return;
        }
        
        MirrorData data = owners.get(owner.getUniqueId());
        if (data == null) {
            data = new MirrorData();
            data.ownerId = owner.getUniqueId();
            owners.put(owner.getUniqueId(), data);
        }
        
        if (data.copiesLeft <= 0) {
            owner.sendMessage("§cВы не можете выдать больше " + MAX_COPIES + " копий!");
            return;
        }
        
        if (copyToOwner.containsKey(target.getUniqueId())) {
            owner.sendMessage("§cУ этого игрока уже есть копия!");
            return;
        }
        
        if (hasFreeSlot(target)) {
            ItemStack copy = MirrorSwordItem.createSword();
            ItemMeta meta = copy.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text("§7§lКопия зеркального меча"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text("§7Копия не имеет способностей"));
                meta.lore(lore);
                copy.setItemMeta(meta);
            }
            
            target.getInventory().addItem(copy);
            
            data.copiesLeft--;
            data.copyHolders.add(target.getUniqueId());
            copyToOwner.put(target.getUniqueId(), owner.getUniqueId());
            
            owner.sendMessage("§aВы выдали копию меча игроку " + target.getName() + "! Осталось копий: " + data.copiesLeft);
            target.sendMessage("§aВы получили копию Зеркального меча от " + owner.getName());
            target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        } else {
            owner.sendMessage("§cУ цели нет свободного места в инвентаре!");
        }
        
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        
        // Если умер владелец
        if (owners.containsKey(dead.getUniqueId())) {
            MirrorData data = owners.get(dead.getUniqueId());
            
            // Оригинальный меч должен выпасть
            for (ItemStack item : dead.getInventory().getContents()) {
                if (item != null && isMirrorSword(item)) {
                    event.getDrops().add(item);
                    dead.getInventory().remove(item);
                    break;
                }
            }
            
            // Все копии исчезают
            for (UUID holderId : data.copyHolders) {
                Player holder = Bukkit.getPlayer(holderId);
                if (holder != null && holder.isOnline()) {
                    for (ItemStack item : holder.getInventory().getContents()) {
                        if (item != null && isCopySword(item)) {
                            holder.getInventory().remove(item);
                            holder.sendMessage("§cВладелец Зеркального меча умер! Ваша копия исчезла.");
                        }
                    }
                    copyToOwner.remove(holderId);
                }
            }
            
            owners.remove(dead.getUniqueId());
        }
        
        // Если умер владелец копии
        if (copyToOwner.containsKey(dead.getUniqueId())) {
            UUID ownerId = copyToOwner.get(dead.getUniqueId());
            MirrorData data = owners.get(ownerId);
            
            if (data != null) {
                // Копия исчезает (не выпадает)
                for (ItemStack item : dead.getInventory().getContents()) {
                    if (item != null && isCopySword(item)) {
                        dead.getInventory().remove(item);
                        break;
                    }
                }
                data.copyHolders.remove(dead.getUniqueId());
                data.copiesLeft++;
                copyToOwner.remove(dead.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Копию нельзя выбросить
        if (isCopySword(item)) {
            event.setCancelled(true);
            player.sendMessage("§cКопию нельзя выбросить!");
            return;
        }
        
        // Если выбросили оригинал
        if (isMirrorSword(item)) {
            UUID ownerId = player.getUniqueId();
            MirrorData data = owners.get(ownerId);
            
            if (data != null) {
                // Удаляем все копии
                for (UUID holderId : data.copyHolders) {
                    Player holder = Bukkit.getPlayer(holderId);
                    if (holder != null && holder.isOnline()) {
                        for (ItemStack invItem : holder.getInventory().getContents()) {
                            if (invItem != null && isCopySword(invItem)) {
                                holder.getInventory().removeItem(invItem);
                                holder.sendMessage("§cОригинальный меч выброшен! Ваша копия исчезла.");
                            }
                        }
                        copyToOwner.remove(holderId);
                    }
                }
                owners.remove(ownerId);
                player.sendMessage("§cВы выбросили Зеркальный меч. Все копии исчезли!");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        // Запрещаем выбрасывать копию через клик за пределы инвентаря
        if (item != null && isCopySword(item) && event.getClickedInventory() == null) {
            event.setCancelled(true);
            player.sendMessage("§cКопию нельзя выбросить!");
        }
    }

    private boolean hasFreeSlot(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    private boolean isMirrorSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.contains("Зеркальный меч") && !name.contains("Копия");
    }
    
    private boolean isCopySword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.contains("Копия");
    }
}