package me.yourname.ultimateitems.commands;

import me.yourname.ultimateitems.items.ItemManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        
        if (args.length > 0 && args[0].equalsIgnoreCase("giveall")) {
            int[] cmds = {101, 102, 103, 104, 105, 106, 107, 108, 109, 201, 204, 205, 206, 207};
            for (int id : cmds) {
                p.getInventory().addItem(ItemManager.getItem(id));
            }
        }
        return true;
    }
}