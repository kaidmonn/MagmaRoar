package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
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
    private final Map<UUID, UUID> copyToOwner = new HashMap<>(); // копия → владелец
    private final Random random = new Random();
    
    private static final int MAX_COPIES = 5;
    private static final long BUFF_COOLDOWN = 60 * 1000; // 60 секунд
    private static final int BUFF_DURATION = 20 * 20; // 20 секунд в тиках

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

        // ПКМ - бафф
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
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
        
        // Проверка расстояния
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
        
        // Проверка лимита копий
        if (data.copiesLeft <= 0) {
            owner.sendMessage("§cВы не можете выдать больше " + MAX_COPIES + " копий!");
            return;
        }
        
        // Проверка, что у цели нет копии
        if (copyToOwner.containsKey(target.getUniqueId())) {
            owner.sendMessage("§cУ этого игрока уже есть копия!");
            return;
        }
        
        // Проверка свободного слота
        if (hasFreeSlot(target)) {
            // Создаём копию (без способностей)
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
            
            // Обновляем данные
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
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Если это копия
        if (copyToOwner.containsKey(player.getUniqueId())) {
            UUID ownerId = copyToOwner.get(player.getUniqueId());
            MirrorData data = owners.get(ownerId);
            
            if (data != null) {
                // Копия просто исчезает
                event.getItemDrop().remove();
                copyToOwner.remove(player.getUniqueId());
                data.copyHolders.remove(player.getUniqueId());
                data.copiesLeft++;
                player.sendMessage("§cКопия исчезла. У владельца освободился слот для новой копии.");
            }
        }
        // Если это оригинал
        else if (isMirrorSword(item)) {
            UUID ownerId = player.getUniqueId();
            MirrorData data = owners.get(ownerId);
            
            if (data != null) {
                // Удаляем все копии
                for (UUID holderId : data.copyHolders) {
                    Player holder = Bukkit.getPlayer(holderId);
                    if (holder != null && holder.isOnline()) {
                        // Ищем копию в инвентаре
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

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Копия неуязвима? Нет, копия — просто предмет, у неё нет защиты
        // Этот метод можно использовать для дополнительных эффектов
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