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
        getCommand("team").setExecutor(new TeamCommand(teamManager));
        getCommand("magmagive").setExecutor(new ItemsCommand());

        // Регистрация всех 14 обработчиков оружия
        var pm = getServer().getPluginManager();
        pm.registerEvents(new Reaper101Handler(), this); // Коса 1
        pm.registerEvents(new Reaper102Handler(), this); // Коса 2
        pm.registerEvents(new Mjolnir103Handler(), this);
        pm.registerEvents(new SculkCrossbow104Handler(), this);
        pm.registerEvents(new DragonKatana105Handler(), this);
        pm.registerEvents(new Mace106Handler(), this);
        pm.registerEvents(new ShadowBlade107Handler(), this);
        pm.registerEvents(new VillagerStaff108Handler(), this);
        pm.registerEvents(new Trident109Handler(), this);
        pm.registerEvents(new BloodSword201Handler(), this); // Меч/Трезубец/Булава
        pm.registerEvents(new Excalibur204Handler(), this);
        pm.registerEvents(new EmeraldBlade205Handler(), this);
        pm.registerEvents(new MidasSword206Handler(), this);
        pm.registerEvents(new TimeBow207Handler(), this);

        getLogger().info("MagmaRoar запущен!");
    }

    public static MagmaRoar getInstance() { return instance; }
    public static TeamManager getTeamManager() { return teamManager; }
}