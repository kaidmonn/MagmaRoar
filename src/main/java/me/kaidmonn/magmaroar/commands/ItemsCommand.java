package me.kaidmonn.magmaroar.commands;

import me.kaidmonn.magmaroar.items.types.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!p.isOp()) return true;

        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("all")) {
            for (ItemStack item : getAllWeapons()) {
                p.getInventory().addItem(item);
            }
        } else if (args[0].equalsIgnoreCase("random")) {
            List<ItemStack> weapons = getAllWeapons();
            for (Player online : Bukkit.getOnlinePlayers()) {
                Collections.shuffle(weapons);
                online.getInventory().addItem(weapons.get(0).clone());
            }
        }
        return true;
    }

    private List<ItemStack> getAllWeapons() {
        List<ItemStack> items = new ArrayList<>();
        items.add(Scythe101Item.getItem());
        items.add(Scythe102Item.getItem());
        items.add(Mjolnir103Item.getItem());
        items.add(SculkBow104Item.getItem());
        items.add(Katana105Item.getItem());
        items.add(Mace106Item.getItem());
        items.add(ShadowBlade107Item.getItem());
        items.add(VillagerStaff108Item.getItem());
        items.add(Trident109Item.getItem());
        items.add(BloodSword201Item.getItem());
        items.add(Excalibur204Item.getItem());
        items.add(EmeraldBlade205Item.getItem());
        items.add(MidasSword206Item.getItem());
        items.add(TimeBow207Item.getItem());
        return items;
    }
}