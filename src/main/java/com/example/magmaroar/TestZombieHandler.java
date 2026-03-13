package com.example.magmaroar;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestZombieHandler implements Listener {

    private final Set<UUID> totemZombies = new HashSet<>();
    private final Set<UUID> shieldZombies = new HashSet<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.ZOMBIE_SPAWN_EGG || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        String name = meta.displayName().toString();
        boolean withTotems = name.contains("Зомби-бессмертный");

        // Спавним зомби
        Zombie zombie = player.getWorld().spawn(player.getLocation(), Zombie.class);
        
        // Отключаем возможность поджечься на солнце
        zombie.setBaby(false);
        zombie.setAdult();
        
        // Полная броня незерит с защитой 4
        EntityEquipment equipment = zombie.getEquipment();
        
        // Шлем
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 4);
        helmet.addEnchantment(Enchantment.UNBREAKING, 3);
        equipment.setHelmet(helmet);
        equipment.setHelmetDropChance(0f);
        
        // Нагрудник
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 4);
        chestplate.addEnchantment(Enchantment.UNBREAKING, 3);
        equipment.setChestplate(chestplate);
        equipment.setChestplateDropChance(0f);
        
        // Штаны
        ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 4);
        leggings.addEnchantment(Enchantment.UNBREAKING, 3);
        equipment.setLeggings(leggings);
        equipment.setLeggingsDropChance(0f);
        
        // Ботинки
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 4);
        boots.addEnchantment(Enchantment.UNBREAKING, 3);
        equipment.setBoots(boots);
        equipment.setBootsDropChance(0f);
        
        if (withTotems) {
            // Бесконечные тотемы
            totemZombies.add(zombie.getUniqueId());
            // Даём тотем в руку
            ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
            equipment.setItemInMainHand(totem);
            equipment.setItemInMainHandDropChance(0f);
            
            player.sendMessage("§cПризван зомби с бесконечными тотемами!");
        } else {
            // Щитоносец
            shieldZombies.add(zombie.getUniqueId());
            
            // Даём щит
            ItemStack shield = new ItemStack(Material.SHIELD);
            shield.addEnchantment(Enchantment.UNBREAKING, 3);
            equipment.setItemInMainHand(shield);
            equipment.setItemInMainHandDropChance(0f);
            
            // Даём меч для атаки
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addEnchantment(Enchantment.SHARPNESS, 5);
            sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            equipment.setItemInOffHand(sword);
            equipment.setItemInOffHandDropChance(0f);
            
            player.sendMessage("§6Призван зомби-щитоносец!");
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (totemZombies.contains(event.getEntity().getUniqueId())) {
            // Зомби с тотемами не должен умирать
            event.setCancelled(true);
            // Лечим его
            ((Zombie) event.getEntity()).setHealth(40);
        }
    }
}