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
import org.jetbrains.annotations.NotNull;

public class TeamCommand implements CommandExecutor {
    private final TeamManager tm;

    public TeamCommand(TeamManager tm) {
        this.tm = tm;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length == 0) return false;

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) return false;
                tm.createTeam(Integer.parseInt(args[1]), p);
                p.sendMessage("§aКоманда создана!");
            }
            case "invite" -> {
                if (args.length < 2) return false;
                Integer teamId = tm.getTeamId(p.getUniqueId());
                if (teamId == null || !tm.getOwner(teamId).equals(p.getUniqueId())) {
                    p.sendMessage("§cВы должны быть лидером команды!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return true;

                tm.addInvite(target.getUniqueId(), teamId);
                
                // Кликабельное сообщение для цели
                Component message = Component.text("§6[MagmaRoar] §fИгрок " + p.getName() + " пригласил вас в команду §e" + teamId + "§f. ")
                    .append(Component.text("§a§l[ПРИНЯТЬ]")
                        .clickEvent(ClickEvent.runCommand("/team accept")))
                    .append(Component.text("  "))
                    .append(Component.text("§c§l[ОТКЛОНИТЬ]")
                        .clickEvent(ClickEvent.runCommand("/team deny")));
                
                target.sendMessage(message);
                p.sendMessage("§aПриглашение отправлено " + target.getName());
            }
            case "accept" -> {
                Integer invitedTeamId = tm.getInvite(p.getUniqueId());
                if (invitedTeamId == null) {
                    p.sendMessage("§cУ вас нет активных приглашений!");
                    return true;
                }
                tm.addPlayer(invitedTeamId, p);
                tm.removeInvite(p.getUniqueId());
                p.sendMessage("§aВы вступили в команду!");
            }
            case "deny" -> {
                tm.removeInvite(p.getUniqueId());
                p.sendMessage("§cВы отклонили приглашение.");
            }
            case "disband" -> {
                Integer id = tm.getTeamId(p.getUniqueId());
                if (id != null && tm.getOwner(id).equals(p.getUniqueId())) tm.disband(id);
            }
            case "leave" -> tm.leaveTeam(p);
        }
        return true;
    }
}