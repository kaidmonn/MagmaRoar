package com.example.dragonstaff;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.EnderDragon;
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

        // Обработка посоха (ПКМ с посохом)
        if (isDragonStaff(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                DragonEntity dragon = DragonEntity.activeDragons.get(player.getUniqueId());

                if (dragon == null || !dragon.isSummoned()) {
                    // Призыв дракона
                    if (DragonEntity.canSummon(player)) {
                        new DragonEntity(player, player.getLocation());
                    } else {
                        long cooldown = DragonEntity.getRemainingCooldown(player);
                        player.sendMessage("§cВы сможете призвать нового дракона через " + cooldown + " сек.");
                    }
                } else {
                    // Атака дракона
                    dragon.attack();
                }
                event.setCancelled(true);
            }
        }
        
        // Обработка взаимодействия с драконом
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // ПКМ по дракону - сесть
            if (event.getClickedBlock() == null && event.getPlayer().getTargetEntity(5) instanceof EnderDragon) {
                EnderDragon dragon = (EnderDragon) event.getPlayer().getTargetEntity(5);
                DragonEntity dragonEntity = DragonEntity.activeDragons.values().stream()
                    .filter(de -> de.getDragon() != null && de.getDragon().equals(dragon))
                    .findFirst().orElse(null);
                
                if (dragonEntity != null && !dragonEntity.isRiding()) {
                    dragonEntity.mountDragon();
                    event.setCancelled(true);
                }
            }
        }
        
        // ЛКМ по дракону - слезть
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() == null && event.getPlayer().getTargetEntity(5) instanceof EnderDragon) {
                EnderDragon dragon = (EnderDragon) event.getPlayer().getTargetEntity(5);
                DragonEntity dragonEntity = DragonEntity.activeDragons.values().stream()
                    .filter(de -> de.getDragon() != null && de.getDragon().equals(dragon))
                    .findFirst().orElse(null);
                
                if (dragonEntity != null && dragonEntity.isRiding()) {
                    dragonEntity.dismountDragon();
                    event.setCancelled(true);
                }
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
    public void onEntityExplode(EntityExplodeEvent event) {
        // Запрещаем разрушение блоков от взрывов нашей вагонетки
        if (event.getEntity() instanceof TNTPrimed) {
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