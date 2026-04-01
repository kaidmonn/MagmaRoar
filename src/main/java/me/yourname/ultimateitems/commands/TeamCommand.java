package me.yourname.ultimateitems.commands;

import me.yourname.ultimateitems.UltimateItems;
import me.yourname.ultimateitems.teams.TeamManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        TeamManager tm = UltimateItems.getInstance().getTeamManager();

        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "create":
                tm.createTeam(p, Integer.parseInt(args[1]));
                p.sendMessage("Команда создана!");
                break;
            case "join":
                // Логика запроса владельцу
                Player owner = Bukkit.getPlayer(args[1]); // Упрощенно
                TextComponent msg = new TextComponent("§aПринять запрос?");
                msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept " + p.getName()));
                owner.spigot().sendMessage(msg);
                break;
            case "disband":
                if (tm.getTeam(p) != null) tm.disband(tm.getTeam(p).getId());
                break;
        }
        return true;
    }
}