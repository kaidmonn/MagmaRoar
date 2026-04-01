package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class EmeraldBlade205Handler implements Listener {

    private final String GUI_NAME = "Улучшение клинка";

    @EventHandler
    public void onOpen(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.DIAMOND_SWORD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 205) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (p.isSneaking()) {
                Inventory gui = Bukkit.createInventory(null, 9, Component.text(GUI_NAME));
                // Открываем пустые слоты (2, 3, 4, 5, 6 для симметрии)
                p.openInventory(gui);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getView().title().equals(Component.text(GUI_NAME))) return;
        
        Player p = (Player) e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() != Material.DIAMOND_SWORD) return;

        int totalEmeralds = 0;
        for (ItemStack slot : e.getInventory().getContents()) {
            if (slot != null && slot.getType() == Material.EMERALD) {
                totalEmeralds += slot.getAmount();
            } else if (slot != null) {
                // Возвращаем не-изумруды игроку
                p.getInventory().addItem(slot).forEach((i, stack) -> 
                    p.getWorld().dropItemNaturally(p.getLocation(), stack));
            }
        }

        // Расчет остроты (64 изумруда = 1 стак)
        int sharpLevel = 0;
        if (totalEmeralds >= 320) sharpLevel = 18; // 5 стаков
        else if (totalEmeralds >= 256) sharpLevel = 15; // 4 стака
        else if (totalEmeralds >= 192) sharpLevel = 12; // 3 стака
        else if (totalEmeralds >= 128) sharpLevel = 8;  // 2 стака
        else if (totalEmeralds >= 64) sharpLevel = 4;   // 1 стак

        applySharpness(item, sharpLevel);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
    }

    private void applySharpness(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (level > 0) {
            meta.addEnchant(Enchantment.SHARPNESS, level, true);
        } else {
            meta.removeEnchant(Enchantment.SHARPNESS);
        }
        item.setItemMeta(meta);
    }
}