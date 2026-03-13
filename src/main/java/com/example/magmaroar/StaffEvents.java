package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaffEvents implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Призыв Рога Магмы (MagmaHorn)
        if (isMagmaHorn(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Здесь логика призыва, если нужна
                player.sendMessage("§6Рог Магмы активирован!");
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        // Логика прыжка для страйдера
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (event.getRightClicked() instanceof Strider) {
            player.sendMessage("§aВы погладили страйдера");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // Логика спешивания
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Логика защиты от взрывов
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Snowball) {
            event.blockList().clear();
        }
    }

    private boolean isMagmaHorn(ItemStack item) {
        if (item != null && item.getType() == Material.GOAT_HORN && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.displayName() != null && 
                   meta.displayName().toString().contains("Рог Магмы");
        }
        return false;
    }
} 