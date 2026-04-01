package me.kaidmonn.magmaroar.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import me.kaidmonn.magmaroar.MagmaRoar;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

public class GiveCommand implements CommandExecutor {

    private final int[] models = {101, 102, 103, 104, 105, 106, 107, 108, 109, 201, 204, 205, 206, 207};

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!p.isOp()) {
            p.sendMessage("§cНет прав!");
            return true;
        }

        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("all")) {
            for (int model : models) {
                p.getInventory().addItem(createWeapon(model));
            }
            p.sendMessage("§aВсе 14 орудий выданы!");
        } 
        
        else if (args[0].equalsIgnoreCase("randomall")) {
            Random r = new Random();
            for (Player online : Bukkit.getOnlinePlayers()) {
                int randomModel = models[r.nextInt(models.length)];
                online.getInventory().addItem(createWeapon(randomModel));
                online.sendMessage("§6Вы получили случайное магическое оружие!");
            }
        }
        return true;
    }

    private ItemStack createWeapon(int model) {
        Material mat = switch (model) {
            case 101, 102 -> Material.NETHERITE_HOE;
            case 103 -> Material.DIAMOND_AXE;
            case 104 -> Material.CROSSBOW;
            case 105, 107, 201, 204 -> Material.NETHERITE_SWORD;
            case 106 -> Material.MACE;
            case 108 -> Material.BLAZE_ROD;
            case 109 -> Material.TRIDENT;
            case 205, 206 -> Material.DIAMOND_SWORD;
            case 207 -> Material.BOW;
            default -> Material.STICK;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(model);
            meta.setUnbreakable(true);
            
            // Устанавливаем урон 14 для мечей/трезубца по ТЗ
            if (model == 105 || model == 107 || model == 109 || model == 201 || model == 204) {
                AttributeModifier modifier = new AttributeModifier(
                    new NamespacedKey(MagmaRoar.getInstance(), "extra_damage"),
                    14.0 - 1.0, // Базовый урон кулака 1.0, ставим в сумме 14
                    AttributeModifier.Operation.ADD_NUMBER
                );
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);
            }

            // Катана Дракона (105) +10% к скорости
            if (model == 105) {
                AttributeModifier speed = new AttributeModifier(
                    new NamespacedKey(MagmaRoar.getInstance(), "katana_speed"),
                    0.1, 
                    AttributeModifier.Operation.ADD_SCALAR
                );
                meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, speed);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}