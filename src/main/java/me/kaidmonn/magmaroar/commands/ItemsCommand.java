package me.kaidmonn.magmaroar.commands;

import me.kaidmonn.magmaroar.items.types.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class ItemsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp() || !(sender instanceof Player p)) return true;

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            for (ItemStack item : getAllWeapons()) p.getInventory().addItem(item);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("random")) {
            List<ItemStack> items = getAllWeapons();
            for (Player online : Bukkit.getOnlinePlayers()) {
                Collections.shuffle(items);
                online.getInventory().addItem(items.get(0));
            }
        }
        return true;
    }

    private List<ItemStack> getAllWeapons() {
        return Arrays.asList(
            Scythe101Item.getItem(), Scythe102Item.getItem(), Mjolnir103Item.getItem(),
            Crossbow104Item.getItem(), Katana105Item.getItem(), Mace106Item.getItem(),
            ShadowBlade107Item.getItem(), VillagerStaff108Item.getItem(), Trident109Item.getItem(),
            BloodSword201Item.getItem(), Excalibur204Item.getItem(), EmeraldBlade205Item.getItem(),
            MidasSword206Item.getItem(), TimeBow207Item.getItem()
        );
    }
}