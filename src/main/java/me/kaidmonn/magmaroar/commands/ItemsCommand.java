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
        if (!sender.isOp()) return true;

        if (args.length == 0) {
            sender.sendMessage("§cИспользование: /magmagive <all|random>");
            return true;
        }

        List<ItemStack> allWeapons = getAllWeapons();

        if (args[0].equalsIgnoreCase("all") && sender instanceof Player p) {
            for (ItemStack item : allWeapons) {
                p.getInventory().addItem(item);
            }
            p.sendMessage("§aВы получили все уникальные оружия!");
        } 
        
        else if (args[0].equalsIgnoreCase("random")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                List<ItemStack> shuffleList = new ArrayList<>(allWeapons);
                Collections.shuffle(shuffleList);
                online.getInventory().addItem(shuffleList.get(0));
                online.sendMessage("§eВы получили случайное уникальное оружие!");
            }
        }

        return true;
    }

    private List<ItemStack> getAllWeapons() {
        List<ItemStack> items = new ArrayList<>();
        items.add(Reaper101Item.getItem());
        items.add(Reaper102Item.getItem());
        items.add(Mjolnir103Item.getItem());
        items.add(SculkCrossbow104Item.getItem());
        items.add(DragonKatana105Item.getItem());
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