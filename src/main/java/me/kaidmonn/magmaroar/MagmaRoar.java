package me.kaidmonn.magmaroar;

import me.kaidmonn.magmaroar.commands.ItemsCommand;
import me.kaidmonn.magmaroar.commands.TeamCommand;
import me.kaidmonn.magmaroar.handlers.*;
import me.kaidmonn.magmaroar.managers.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MagmaRoar extends JavaPlugin {

    private static MagmaRoar instance;
    private static TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;
        teamManager = new TeamManager();

        // Регистрация команд
        if (getCommand("team") != null) {
            getCommand("team").setExecutor(new TeamCommand(teamManager));
        }
        if (getCommand("magmagive") != null) {
            getCommand("magmagive").setExecutor(new ItemsCommand());
        }

        // Регистрация всех обработчиков (названия исправлены согласно логам компиляции)
        var pm = getServer().getPluginManager();
        
        // 101-109
        pm.registerEvents(new Scythe101Handler(), this);
        pm.registerEvents(new Scythe102Handler(), this);
        pm.registerEvents(new Mjolnir103Handler(), this);
        pm.registerEvents(new Crossbow104Handler(), this);
        pm.registerEvents(new Katana105Handler(), this);
        pm.registerEvents(new Mace106Handler(), this);
        pm.registerEvents(new ShadowBlade107Handler(), this);
        pm.registerEvents(new VillagerStaff108Handler(), this);
        pm.registerEvents(new Trident109Handler(), this);
        
        // 201-207
        pm.registerEvents(new BloodSword201Handler(), this);
        pm.registerEvents(new Excalibur204Handler(), this);
        pm.registerEvents(new EmeraldBlade205Handler(), this);
        pm.registerEvents(new MidasSword206Handler(), this);
        pm.registerEvents(new TimeBow207Handler(), this);

        getLogger().info("MagmaRoar успешно запущен!");
    }

    public static MagmaRoar getInstance() {
        return instance;
    }

    public static TeamManager getTeamManager() {
        return teamManager;
    }
}