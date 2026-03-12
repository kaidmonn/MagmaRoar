package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaffEvents implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isMagmaHorn(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                MagmaRoar roar = MagmaRoar.activeMagmaRoars.get(player.getUniqueId());

                if (roar == null || !roar.isSummoned()) {
                    if (MagmaRoar.canSummon(player)) {
                        new MagmaRoar(player, player.getLocation());
                    } else {
                        long cooldown = MagmaRoar.getRemainingCooldown(player);
                        player.sendMessage("§cВы сможете призвать нового Магма Рёва через " + cooldown + " сек.");
                    }
                } else {
                    roar.attack();
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (event.getRightClicked() instanceof Strider) {
            Strider strider = (Strider) event.getRightClicked();
            MagmaRoar roar = MagmaRoar.activeMagmaRoars.values().stream()
                .filter(r -> r.getStrider() != null && r.getStrider().equals(strider))
                .findFirst().orElse(null);
            
            if (roar != null && !roar.isRiding()) {
                roar.mount(player);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (event.isSneaking() && player.getVehicle() instanceof Strider) {
            MagmaRoar roar = MagmaRoar.activeMagmaRoars.get(player.getUniqueId());
            if (roar != null && roar.isRiding()) {
                roar.dismount();
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            
            if (event.getEntity() instanceof Strider) {
                MagmaRoar roar = MagmaRoar.activeMagmaRoars.values().stream()
                    .filter(r -> r.getStrider() != null && r.getStrider().equals(event.getEntity()))
                    .findFirst().orElse(null);
                if (roar != null) {
                    event.setCancelled(true);
                }
            }
            
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (player.getVehicle() instanceof Strider) {
                    MagmaRoar roar = MagmaRoar.activeMagmaRoars.get(player.getUniqueId());
                    if (roar != null && roar.isRiding()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
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