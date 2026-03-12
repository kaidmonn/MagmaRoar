package com.example.dragonstaff;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
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
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());
        
        if (dragon != null && dragon.isSummoned() && dragon.isRiding()) {
            dragon.toggleHover();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());

        if (dragon != null && dragon.isSummoned()) {
            if (player.getVehicle() != null && player.getVehicle().equals(dragon.getDragon())) {
                if (dragon.getDragon().isOnGround() && event.isSneaking()) {
                    dragon.dismountDragon();
                }
            } else if (!dragon.isRiding() && event.isSneaking()) {
                if (dragon.getDragon() != null && player.getLocation().distance(dragon.getDragon().getLocation()) < 3) {
                    dragon.mountDragon();
                }
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof org.bukkit.entity.EnderDragon && event.getExited() instanceof Player) {
            Player player = (Player) event.getExited();
            DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());

            if (dragon != null && dragon.isRiding() && !player.isSneaking()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.TNTPrimed || 
            event.getEntity() instanceof org.bukkit.entity.FallingBlock) {
            event.blockList().clear();
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