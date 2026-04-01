package me.kaidmonn.magmaroar.handlers;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public class MidasSword206Handler implements Listener {

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_SWORD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 206) return;

        var random = ThreadLocalRandom.current();

        if (e.getEntity() instanceof Player victim) {
            // Дроп для игрока: 2-7 блоков золота
            int blocks = random.nextInt(2, 8); // от 2 до 7 включительно
            e.getDrops().add(new ItemStack(Material.GOLD_BLOCK, blocks));
            
            // Прокачка
            upgradeMidas(killer, item);
        } else {
            // Дроп для моба: 20-55 самородков
            int nuggets = random.nextInt(20, 56); // от 20 до 55 включительно
            e.getDrops().add(new ItemStack(Material.GOLD_NUGGET, nuggets));
        }
        
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
    }

    private void upgradeMidas(Player p, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        int currentLevel = meta.getEnchantLevel(Enchantment.SHARPNESS);

        if (currentLevel < 25) {
            int nextLevel = currentLevel + 1;
            meta.addEnchant(Enchantment.SHARPNESS, nextLevel, true);
            item.setItemMeta(meta);
            
            p.sendActionBar(org.bukkit.ChatColor.GOLD + "Сила Мидаса растет! Острота: " + nextLevel);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.8f);
        }
    }
}