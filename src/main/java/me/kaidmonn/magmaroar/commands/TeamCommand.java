package me.kaidmonn.magmaroar.commands;

import me.kaidmonn.magmaroar.managers.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {
    private final TeamManager tm;

    public TeamCommand(TeamManager tm) { this.tm = tm; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "create":
                int id = Integer.parseInt(args[1]);
                tm.createTeam(id, p);
                p.sendMessage("Команда " + id + " создана.");
                break;

            case "join":
                int joinId = Integer.parseInt(args[1]);
                Player owner = Bukkit.getPlayer(tm.getOwner(joinId));
                if (owner != null) {
                    owner.sendMessage(Component.text("Игрок " + p.getName() + " хочет в команду. ")
                        .append(Component.text("[ПРИНЯТЬ]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/team acceptjoin " + p.getName()))));
                }
                break;

            case "invite":
                Player target = Bukkit.getPlayer(args[1]);
                Integer tId = tm.getTeamId(p.getUniqueId());
                if (target != null && tId != null) {
                    target.sendMessage(Component.text("Вас пригласили в команду " + tId + ". ")
                        .append(Component.text("[ПРИНЯТЬ]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/team acceptinvite " + tId))));
                }
                break;

            case "acceptjoin":
                // Техническая подкоманда для клика
                Player applicant = Bukkit.getPlayer(args[1]);
                Integer leaderTeam = tm.getTeamId(p.getUniqueId());
                if (applicant != null && leaderTeam != null) tm.addPlayer(leaderTeam, applicant);
                break;

            case "acceptinvite":
                tm.addPlayer(Integer.parseInt(args[1]), p);
                break;

            case "disband":
                Integer dId = tm.getTeamId(p.getUniqueId());
                if (dId != null && tm.getOwner(dId).equals(p.getUniqueId())) tm.disband(dId);
                break;
            
            case "leave":
                tm.leaveTeam(p);
                break;
        }
        return true;
    }
}