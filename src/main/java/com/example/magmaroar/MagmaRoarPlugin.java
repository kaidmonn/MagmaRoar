package com.example.magmaroar;

import org.bukkit.plugin.java.JavaPlugin;

public class MagmaRoarPlugin extends JavaPlugin {

    private static MagmaRoarPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new StaffEvents(), this);
        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(MagmaHornItem.createHorn());
            }
            return true;
        });
        getLogger().info("MagmaRoarPlugin включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagmaRoarPlugin выключен.");
    }

    public static MagmaRoarPlugin getInstance() {
        return instance;
    }
}