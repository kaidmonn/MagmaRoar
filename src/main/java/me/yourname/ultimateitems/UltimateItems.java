package me.yourname.ultimateitems;

import me.yourname.ultimateitems.commands.TeamCommand;
import me.yourname.ultimateitems.commands.AdminCommand;
import me.yourname.ultimateitems.listeners.WeaponListener;
import me.yourname.ultimateitems.listeners.TeamListener;
import me.yourname.ultimateitems.teams.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimateItems extends JavaPlugin {
    private static UltimateItems instance;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация менеджера команд
        this.teamManager = new TeamManager();
        
        // Регистрация команд
        if (getCommand("team") != null) {
            getCommand("team").setExecutor(new TeamCommand());
        }
        if (getCommand("uitems") != null) {
            getCommand("uitems").setExecutor(new AdminCommand());
        }
        
        // Регистрация всех слушателей (Weapon и Team)
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(), this);

        getLogger().info("UltimateItems v1.0 (1.21.4) успешно включен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("UltimateItems выключен. Данные сессии очищены.");
    }

    public static UltimateItems getInstance() {
        return instance;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}