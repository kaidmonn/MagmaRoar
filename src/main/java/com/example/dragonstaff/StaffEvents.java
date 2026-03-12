package com.example.dragonstaff;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaffEvents implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isDragonStaff(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());

                if (dragon == null || !dragon.isSummoned()) {
                    if (DragonEntity.canSummon(player)) {
                        new DragonEntity(player, player.getLocation());
                    } else {
                        long cooldown = DragonEntity.getRemainingCooldown(player);
                        player.sendMessage("§cВы сможете призвать нового дракона через " + cooldown + " сек.");
                    }
                } else {
                    dragon.attack();
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // ПКМ по дракону - сесть
        if (event.getRightClicked() instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon) event.getRightClicked();
            DragonEntity dragonEntity = DragonEntity.activeDragons.values().stream()
                .filter(de -> de.getDragon() != null && de.getDragon().equals(dragon))
                .findFirst().orElse(null);
            
            if (dragonEntity != null && !dragonEntity.isRiding()) {
                dragonEntity.mountDragon();
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());
        
        if (dragon != null && dragon.isSummoned() && dragon.isRiding()) {
            dragon.toggleHover();
            event.setCancelled(true);
        }
    }

    private boolean isDragonStaff(ItemStack item) {
        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.displayName() != null && 
                   meta.displayName().toString().contains("Посох Дракона");
        }
        return false;
    }
}